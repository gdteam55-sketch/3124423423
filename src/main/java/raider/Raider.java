package raider;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.File;
import java.util.*;

/**
 * Raider v3.
 *  - Ядро (маяк) = база и ВОЕННЫЙ МАГАЗИН (ПКМ по маяку, оплата землёй).
 *  - Зенитка = отдельный предмет, 20 уровней, 1..20 выстрелов в секунду.
 *  - Волны врагов с моделями (танк, самолёт, миномёт, дрон, десант), боссбар, сирена.
 *  - Жители-ополченцы: оружие, броня, навыки, ремонт стен, эмоции над головой.
 *  - Рация (синие союзники), пульт ядерной бомбы, оружие, патроны, гранаты, лопаты.
 *  - Все числа: plugins/Raider/config.yml или /raider set <ключ> <значение>.
 */
public final class Raider extends JavaPlugin implements Listener {

    static final Material CORE_MAT = Material.BEACON;
    static final Material TURRET_MAT = Material.POLISHED_DEEPSLATE;

    static final EntityType[][] TIERS = {
            {EntityType.ZOMBIE, EntityType.SKELETON},
            {EntityType.SPIDER, EntityType.CREEPER, EntityType.HUSK},
            {EntityType.WITCH, EntityType.STRAY, EntityType.CAVE_SPIDER, EntityType.DROWNED, EntityType.PILLAGER},
            {EntityType.VINDICATOR, EntityType.SLIME, EntityType.BOGGED, EntityType.ZOMBIFIED_PIGLIN, EntityType.ENDERMAN},
            {EntityType.BLAZE, EntityType.WITHER_SKELETON, EntityType.MAGMA_CUBE, EntityType.PHANTOM, EntityType.BREEZE},
            {EntityType.RAVAGER, EntityType.EVOKER, EntityType.VEX, EntityType.ZOGLIN},
            {EntityType.PIGLIN_BRUTE, EntityType.SILVERFISH, EntityType.ENDERMITE, EntityType.RAVAGER, EntityType.EVOKER}
    };
    static final EntityType[] ALLY_TYPES = {
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.HUSK, EntityType.STRAY, EntityType.SPIDER,
            EntityType.DROWNED, EntityType.VINDICATOR, EntityType.PILLAGER, EntityType.WITHER_SKELETON,
            EntityType.PIGLIN_BRUTE, EntityType.RAVAGER, EntityType.EVOKER};
    static final String[] STRANGE = {"Искажённый", "Мутант", "Пустотный", "Ржавый", "Безликий", "Древний", "Шепчущий"};

    // ===== Зенитка: 20 уровней, у каждого свой облик =====
    static final String[] AA_NAMES = {
            "Зенитная хлопушка", "Ржавый зенит", "Пушка-малютка", "Двустволка", "Каменный бастион",
            "Железный страж", "Медный залп", "Золотой шторм", "Редстоун-батарея", "Кварцевый ёж",
            "Изумрудный ярус", "Лазурная артиллерия", "Алмазный шквал", "Пурпурный разрушитель", "Незер-крепость",
            "Призма-пушка", "Эндер-цитадель", "Обсидиановый гигант", "Незеритовый титан", "Маяк Апокалипсиса"};
    static final Material[] AA_BODY = {
            Material.COBBLESTONE, Material.MOSSY_COBBLESTONE, Material.STONE_BRICKS, Material.POLISHED_ANDESITE,
            Material.DEEPSLATE_BRICKS, Material.IRON_BLOCK, Material.COPPER_BLOCK, Material.GOLD_BLOCK,
            Material.REDSTONE_BLOCK, Material.QUARTZ_BLOCK, Material.EMERALD_BLOCK, Material.LAPIS_BLOCK,
            Material.DIAMOND_BLOCK, Material.PURPUR_BLOCK, Material.RED_NETHER_BRICKS, Material.PRISMARINE_BRICKS,
            Material.END_STONE_BRICKS, Material.OBSIDIAN, Material.NETHERITE_BLOCK, Material.CRYING_OBSIDIAN};
    static final Material[] AA_HEAD = {
            Material.DISPENSER, Material.DROPPER, Material.OBSERVER, Material.PISTON, Material.STICKY_PISTON,
            Material.BLAST_FURNACE, Material.SMITHING_TABLE, Material.TARGET, Material.REDSTONE_LAMP, Material.LODESTONE,
            Material.SEA_LANTERN, Material.GLOWSTONE, Material.SHROOMLIGHT, Material.RESPAWN_ANCHOR, Material.ENCHANTING_TABLE,
            Material.CONDUIT, Material.END_PORTAL_FRAME, Material.ANCIENT_DEBRIS, Material.BEACON, Material.DRAGON_EGG};
    static final Material[] AA_BARREL = {
            Material.STONE, Material.ANDESITE, Material.GRAY_CONCRETE, Material.LIGHT_GRAY_CONCRETE, Material.IRON_BLOCK,
            Material.ORANGE_CONCRETE, Material.YELLOW_CONCRETE, Material.LIME_CONCRETE, Material.RED_CONCRETE, Material.WHITE_CONCRETE,
            Material.GREEN_CONCRETE, Material.BLUE_CONCRETE, Material.CYAN_CONCRETE, Material.LIGHT_BLUE_CONCRETE, Material.MAGENTA_CONCRETE,
            Material.PURPLE_CONCRETE, Material.PINK_CONCRETE, Material.BROWN_CONCRETE, Material.BLACK_CONCRETE, Material.WHITE_CONCRETE};

    // ===== Ополчение =====
    static final String[] WNAMES = {"Снежки и палки", "Пистолет", "Автомат", "Пулемёт", "Гранатомёт"};
    static final int[] WINT = {14, 10, 5, 3, 70};
    static final double[] WDMG = {0, 4.0, 4.5, 5.5, 0};
    static final double[] WSPREAD = {0.06, 0.05, 0.045, 0.07, 0.0};
    static final String[] SKILLS = {"Меткость", "Скорострельность", "Живучесть", "Ремонт"};

    static final Map<String, String[]> EMO = new HashMap<>();
    static {
        EMO.put("repair", new String[]{"Опять стену разнесли... Чинить придётся из-за этих мерзавцев!", "Кирпичик к кирпичику... из-за этих негодяев.", "Сколько можно ломать?! Чиню...", "Кто это построит, если не я?"});
        EMO.put("wallbreak", new String[]{"Ааа! Стену снесли!", "Они разнесли нашу стену! Мерзавцы!", "Опять ремонт... из-за этих дикарей!"});
        EMO.put("fire", new String[]{"Получай!", "Вот тебе!", "Не подходи!", "Держи гостинец!"});
        EMO.put("kill", new String[]{"Ха! Один готов!", "Так их!", "Минус один!"});
        EMO.put("hurt", new String[]{"Ай! Больно!", "Ох, задели!", "Мамочки!"});
        EMO.put("wavestart", new String[]{"Они идут! К оружию!", "Тревога! Все по местам!", "Опять эти... Держимся!"});
        EMO.put("wavedone", new String[]{"Фух... отбились.", "Победа! Теперь можно чинить.", "Все целы? Ну и славно."});
        EMO.put("bomber", new String[]{"Самолёт! В укрытие!", "Бомбят! Прячьтесь!"});
        EMO.put("tank", new String[]{"Танк! Нам конец!", "Это же танк!.."});
        EMO.put("nuke", new String[]{"Бежим! Сейчас рванёт!", "Мамочки, ядерная!"});
        EMO.put("upgrade", new String[]{"Я чувствую себя сильнее!", "Спасибо, командир!"});
        EMO.put("armed", new String[]{"Ого, оружие! Спасибо!", "Теперь-то мы им покажем!"});
        EMO.put("enlist", new String[]{"Я готов защищать базу!", "Записываюсь в ополчение!"});
        EMO.put("ally", new String[]{"Подкрепление! Ура!", "Наши синие пришли!"});
        EMO.put("lost", new String[]{"Нет... они погибли!", "Мы теряем людей..."});
        EMO.put("idle", new String[]{"Тишина... подозрительно.", "Хорошо тут у нас."});
        EMO.put("turret", new String[]{"Зенитка стала мощнее!", "Теперь небо под защитой!"});
    }

    static final String[][] SHOP = {
            {"turret", "10", "1"}, {"vest_light", "11", "1"}, {"vest_heavy", "12", "1"}, {"vest_assault", "13", "1"},
            {"helmet", "14", "1"}, {"boots", "15", "1"},
            {"rifle", "19", "1"}, {"mg", "20", "1"}, {"sniper", "21", "1"}, {"ammo", "22", "32"},
            {"grenade", "23", "4"}, {"grenade_fire", "24", "4"},
            {"shovel_iron", "28", "1"}, {"shovel_diamond", "29", "1"}, {"shovel_netherite", "30", "1"},
            {"radio", "31", "1"}, {"nuke", "34", "1"}};
    static final List<String> ITEM_IDS = List.of("core", "turret", "vest_light", "vest_heavy", "vest_assault", "helmet", "boots",
            "rifle", "mg", "sniper", "ammo", "grenade", "grenade_fire", "shovel_iron", "shovel_diamond", "shovel_netherite",
            "radio", "nuke");

    static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("count", "attackers.base-count"), Map.entry("perwave", "attackers.per-wave"),
            Map.entry("mult", "attackers.multiplier"), Map.entry("maxalive", "attackers.max-alive"),
            Map.entry("siren", "siren.enabled"), Map.entry("wavetime", "timing.wave-seconds"),
            Map.entry("villagers", "villagers.help"), Map.entry("hp", "enemy.health-multiplier"),
            Map.entry("speed", "enemy.speed-multiplier"), Map.entry("damage", "enemy.damage-multiplier"),
            Map.entry("nukeradius", "nuke.radius"));

    enum State { WAIT, NEGOTIATE, WAR, PEACE }

    static final class Rep { final Location loc; final BlockData data; Rep(Location l, BlockData d) { loc = l; data = d; } }

    static final class Base {
        String world; int x, y, z;
        State state = State.WAIT;
        int countdown = 30;
        int wave = 0;
        BossBar bar;
        int waveTotal = 0, alive = 0;
        boolean spawning = false, cleared = true;
        final List<Rep> repairs = new ArrayList<>();
        Base(String w, int x, int y, int z) { world = w; this.x = x; this.y = y; this.z = z; }
        String key() { return world + ";" + x + ";" + y + ";" + z; }
        World w() { return Bukkit.getWorld(world); }
        Location loc() { return new Location(w(), x + 0.5, y, z + 0.5); }
    }

    static final class Turret {
        String world; int x, y, z;
        int level = 1, dirt = 0, modelLevel = -1;
        double acc = 0;
        Entity target;
        final List<BlockDisplay> parts = new ArrayList<>();
        Turret(String w, int x, int y, int z) { world = w; this.x = x; this.y = y; this.z = z; }
        String key() { return world + ";" + x + ";" + y + ";" + z; }
        World w() { return Bukkit.getWorld(world); }
        Location top() { return new Location(w(), x + 0.5, y + 1.9 + 0.05 * level, z + 0.5); }
    }

    static final class Mdl {
        String kind; int age = 0;
        final List<BlockDisplay> parts = new ArrayList<>();
        double lx, ly, lz; float lyaw = 9999;
    }

    static final class Stuck { Location last; int bad; int fly; }

    static final class Menu implements InventoryHolder {
        final String type; final UUID target; Inventory inv;
        Menu(String type, UUID target) { this.type = type; this.target = target; }
        @Override public Inventory getInventory() { return inv; }
    }

    NamespacedKey CORE, ROLE, BASE, MODEL, AA, DEF, THROW, ITEM, TLEVEL, BULLET, GREN, ALLY,
            V_WEAPON, V_ARMOR, V_AIM, V_RATE, V_HP, V_REPAIR;
    final Map<String, Base> bases = new HashMap<>();
    final Map<String, Turret> turrets = new HashMap<>();
    final Set<UUID> mobs = new HashSet<>();
    final Map<UUID, Integer> bees = new HashMap<>();
    final Set<UUID> defenders = new HashSet<>();
    final Map<UUID, Long> allies = new HashMap<>();
    final Map<UUID, Mdl> models = new HashMap<>();
    final Map<UUID, Stuck> stuck = new HashMap<>();
    final Map<UUID, Long> nextShot = new HashMap<>();
    final Map<UUID, Long> nextRepair = new HashMap<>();
    final Map<UUID, Long> lastEmote = new HashMap<>();
    final Map<UUID, Long> playerShot = new HashMap<>();
    final Map<UUID, Long> radioCd = new HashMap<>();
    final Map<String, String> DESC = new LinkedHashMap<>();
    final Random rnd = new Random();
    List<LivingEntity> enemyCache = new ArrayList<>();
    List<Entity> airCache = new ArrayList<>();
    List<LivingEntity> allyCache = new ArrayList<>();
    Team allyTeam;
    File dataFile;
    int c = 0;
    int bubbles = 0;
    int bombersThisWave = 0;
    boolean nukeBusy = false;

    long now() { return c * 2L; }

    // ===== Конфиг =====
    int ci(String k) { return getConfig().getInt(k); }
    double cd(String k) { return getConfig().getDouble(k); }
    boolean cb(String k) { return getConfig().getBoolean(k); }
    int minDist() { return ci("spawn.min-distance"); }
    int maxDist() { return Math.max(ci("spawn.max-distance"), minDist() + 1); }

    void d(String k, Object v, String desc) { getConfig().addDefault(k, v); DESC.put(k, desc); }

    void loadDefaults() {
        if (getConfig().getInt("config-version", 0) < 3) {
            for (String k : new ArrayList<>(getConfig().getKeys(false))) getConfig().set(k, null);
        }
        d("config-version", 3, "Служебное поле, не трогай");
        d("attackers.base-count", 20, "Сколько врагов в 1-й волне");
        d("attackers.per-wave", 8, "На сколько больше врагов с каждой волной");
        d("attackers.multiplier", 1.0, "Общий множитель числа врагов (2.0 = вдвое больше)");
        d("attackers.max-per-wave", 200, "Потолок врагов в одной волне");
        d("attackers.max-alive", 400, "Потолок живых врагов одновременно");
        d("attackers.special-chance", 0.35, "Шанс, что враг особый (техника, снайпер и т.д.)");
        d("attackers.kamikaze-base", 4, "Смертники (пчёлы/дроны) со 2-й волны: стартовое число");
        d("attackers.kamikaze-per-wave", 2, "Прирост смертников за волну");
        d("attackers.kamikaze-max", 60, "Максимум смертников за волну");
        d("attackers.spawn-batch", 8, "Сколько врагов появляется за один шаг (меньше = меньше лагов)");
        d("enemy.health-multiplier", 1.0, "Множитель здоровья врагов");
        d("enemy.speed-multiplier", 1.0, "Множитель скорости врагов");
        d("enemy.damage-multiplier", 1.0, "Множитель урона врагов (мили)");
        d("enemy.follow-range", 128, "Дальность, с которой враги видят цель");
        d("enemy.mutations", true, "Странные мутации врагов (гиганты, свечение, эффекты)");
        d("enemy.riders-from-wave", 10, "С какой волны враги приходят верхом друг на друге");
        d("specials.drone-from-wave", 5, "С какой волны дроны-смертники вместо пчёл");
        d("specials.gunner-from-wave", 4, "С какой волны автоматчики");
        d("specials.sniper-from-wave", 6, "С какой волны снайперы");
        d("specials.bomber-from-wave", 6, "С какой волны бомбардировщики");
        d("specials.tank-from-wave", 7, "С какой волны танки");
        d("specials.para-from-wave", 8, "С какой волны десант с парашютами");
        d("specials.mortar-from-wave", 9, "С какой волны миномёты");
        d("specials.max-bombers", 6, "Максимум бомбардировщиков за волну");
        d("specials.tank-health", 150, "Здоровье танка");
        d("specials.bomber-health", 40, "Здоровье бомбардировщика");
        d("specials.gunner-damage", 2.0, "Урон пули автоматчика");
        d("specials.sniper-damage", 9.0, "Урон снайпера");
        d("specials.tank-reload-ticks", 30, "Перезарядка танка (тиков)");
        d("specials.mortar-reload-ticks", 50, "Перезарядка миномёта (тиков)");
        d("specials.bomb-interval-ticks", 12, "Интервал бомб бомбардировщика (тиков)");
        d("specials.bee-explosion", 3.5, "Мощность взрыва пчелы-смертника");
        d("specials.drone-explosion", 4.5, "Мощность взрыва дрона");
        d("spawn.min-distance", 50, "Минимум блоков от базы до спавна");
        d("spawn.max-distance", 75, "Максимум блоков от базы до спавна");
        d("timing.wait-seconds", 30, "Секунд от установки ядра до переговорщиков");
        d("timing.negotiate-seconds", 120, "Секунд на переговоры");
        d("timing.wave-seconds", 90, "Секунд между волнами");
        d("bar.enabled", true, "Полоса волны сверху экрана");
        d("bar.radius", 250, "В каком радиусе от базы игроки видят полосу");
        d("siren.enabled", true, "Сирена воздушной тревоги во время волны");
        d("siren.radius", 150, "Радиус слышимости сирены");
        d("siren.volume", 1.0, "Громкость сирены");
        d("siren.cycle-seconds", 14, "Секунд на один цикл вой вверх-вниз");
        d("siren.min-pitch", 0.55, "Нижний тон сирены (0.5..2.0)");
        d("siren.max-pitch", 1.75, "Верхний тон сирены (0.5..2.0)");
        d("turret.shots-per-level", 1.0, "Выстрелов в секунду на каждый уровень (ур.20 = 20 выстр./сек)");
        d("turret.range-base", 24, "Дальность зенитки на 1 уровне");
        d("turret.range-per-level", 2.5, "Прирост дальности за уровень");
        d("turret.yield-base", 0.4, "Мощность взрыва фаербола, база");
        d("turret.yield-per-level", 0.1, "Прирост мощности за уровень");
        d("turret.fireball-speed", 2.0, "Скорость фаерболов");
        d("turret.upgrade-base", 8, "Земли на улучшение: база");
        d("turret.upgrade-per-level", 8, "Земли на улучшение: плюс за уровень");
        d("villagers.help", true, "Жители помогают в бою");
        d("villagers.max", 16, "Максимум автоматически вступающих жителей");
        d("villagers.chance", 0.5, "Шанс, что житель вступит в ополчение в начале волны");
        d("villagers.throw-damage", 2, "Урон снежка");
        d("villagers.stick-damage", 3, "Урон палки");
        d("villagers.repair", true, "Жители чинят разрушенные блоки");
        d("villagers.repair-interval-ticks", 10, "Тиков на починку одного блока");
        d("villagers.repair-radius", 70, "Радиус записи разрушений вокруг базы");
        d("villagers.repair-max-queue", 3000, "Максимум блоков в очереди ремонта");
        d("villagers.emotions", true, "Реплики над головой жителей");
        d("villagers.emote-cooldown-ticks", 80, "Пауза между репликами одного жителя");
        d("villagers.armor-reduction-per-tier", 0.15, "Снижение урона за уровень брони");
        d("villagers.hp-per-level", 8, "Здоровья за уровень навыка Живучесть");
        d("militia.enlist-cost", 20, "Земли: записать в ополчение");
        d("militia.weapon-cost", 50, "Земли: оружие (умножается на следующий уровень)");
        d("militia.armor-cost", 40, "Земли: броня (умножается на следующий уровень)");
        d("militia.skill-cost", 15, "Земли: навык (умножается на следующий уровень)");
        d("militia.skill-max", 5, "Максимальный уровень навыка");
        d("weapons.rifle-damage", 4.0, "Урон автомата");
        d("weapons.mg-damage", 5.0, "Урон пулемёта");
        d("weapons.sniper-damage", 14.0, "Урон снайперки");
        d("weapons.rifle-interval-ticks", 3, "Интервал выстрелов автомата");
        d("weapons.mg-interval-ticks", 2, "Интервал выстрелов пулемёта");
        d("weapons.sniper-interval-ticks", 30, "Интервал выстрелов снайперки");
        d("weapons.bullet-speed", 4.0, "Скорость пули");
        d("weapons.grenade-power", 3.0, "Мощность осколочной гранаты");
        d("weapons.grenade-fire-power", 2.0, "Мощность зажигательной гранаты");
        d("shovel.bonus-iron", 1, "Лишней земли с блока: железная лопата");
        d("shovel.bonus-diamond", 2, "Лишней земли с блока: алмазная лопата");
        d("shovel.bonus-netherite", 4, "Лишней земли с блока: незеритовая лопата");
        d("nuke.enabled", true, "Разрешить ядерную бомбу");
        d("nuke.radius", 40, "Радиус воронки");
        d("nuke.delay-seconds", 5, "Обратный отсчёт до взрыва");
        d("nuke.kill-players", true, "Убивать игроков в зоне взрыва");
        d("nuke.depth-factor", 0.5, "Глубина воронки (доля от радиуса)");
        d("radio.count", 8, "Сколько союзников приходит по рации");
        d("radio.per-3-waves", 1, "Прибавка союзников за каждые 3 волны");
        d("radio.cooldown-seconds", 120, "Перезарядка рации");
        d("radio.lifetime-seconds", 240, "Сколько живут союзники");
        d("radio.health-multiplier", 1.5, "Множитель здоровья союзников");
        d("chest-chance", 7, "% шанс найти ядро в сундуке");
        d("prices.turret", 150, "Цена: зенитка (земли)");
        d("prices.vest_light", 80, "Цена: лёгкий бронежилет");
        d("prices.vest_heavy", 200, "Цена: тяжёлый бронежилет");
        d("prices.vest_assault", 450, "Цена: штурмовой бронежилет");
        d("prices.helmet", 60, "Цена: каска");
        d("prices.boots", 50, "Цена: берцы");
        d("prices.rifle", 180, "Цена: автомат");
        d("prices.mg", 400, "Цена: пулемёт");
        d("prices.sniper", 350, "Цена: снайперская винтовка");
        d("prices.ammo", 20, "Цена: 32 патрона");
        d("prices.grenade", 40, "Цена: 4 осколочные гранаты");
        d("prices.grenade_fire", 45, "Цена: 4 зажигательные гранаты");
        d("prices.shovel_iron", 40, "Цена: железная лопата");
        d("prices.shovel_diamond", 120, "Цена: алмазная лопата");
        d("prices.shovel_netherite", 300, "Цена: незеритовая лопата");
        d("prices.radio", 250, "Цена: рация");
        d("prices.nuke", 800, "Цена: пульт ядерной бомбы");
        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    // ===== Жизненный цикл =====
    @Override
    public void onEnable() {
        CORE = new NamespacedKey(this, "core");
        ROLE = new NamespacedKey(this, "role");
        BASE = new NamespacedKey(this, "base");
        MODEL = new NamespacedKey(this, "model");
        AA = new NamespacedKey(this, "aa");
        DEF = new NamespacedKey(this, "defender");
        THROW = new NamespacedKey(this, "vthrow");
        ITEM = new NamespacedKey(this, "item");
        TLEVEL = new NamespacedKey(this, "tlevel");
        BULLET = new NamespacedKey(this, "bullet");
        GREN = new NamespacedKey(this, "grenade");
        ALLY = new NamespacedKey(this, "ally");
        V_WEAPON = new NamespacedKey(this, "v_weapon");
        V_ARMOR = new NamespacedKey(this, "v_armor");
        V_AIM = new NamespacedKey(this, "v_aim");
        V_RATE = new NamespacedKey(this, "v_rate");
        V_HP = new NamespacedKey(this, "v_hp");
        V_REPAIR = new NamespacedKey(this, "v_repair");
        loadDefaults();
        initTeam();
        getDataFolder().mkdirs();
        dataFile = new File(getDataFolder(), "data.yml");
        load();
        Bukkit.getPluginManager().registerEvents(this, this);
        for (World w : Bukkit.getWorlds()) for (Entity en : w.getEntities()) rescan(en);
        Bukkit.getScheduler().runTaskTimer(this, this::secondTick, 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(this, this::fastTick, 2L, 2L);
        Bukkit.getScheduler().runTaskTimer(this, this::turretTick, 1L, 1L);
        getLogger().info("Raider v3 включён. Баз: " + bases.size() + ", зениток: " + turrets.size());
    }

    @Override
    public void onDisable() {
        for (Base b : bases.values()) if (b.bar != null) b.bar.removeAll();
        for (UUID id : new ArrayList<>(allies.keySet())) { Entity e = Bukkit.getEntity(id); if (e != null) e.remove(); }
        for (Mdl m : models.values()) for (BlockDisplay part : m.parts) part.remove();
        for (Turret t : turrets.values()) for (BlockDisplay part : t.parts) part.remove();
        save();
    }

    void initTeam() {
        try {
            Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
            allyTeam = sb.getTeam("raider_allies");
            if (allyTeam == null) allyTeam = sb.registerNewTeam("raider_allies");
            allyTeam.setColor(ChatColor.BLUE);
        } catch (Throwable t) { getLogger().warning("Команда союзников не создана: " + t.getMessage()); }
    }

    // ===== Сохранение =====
    void save() {
        YamlConfiguration y = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Base b : bases.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("world", b.world); m.put("x", b.x); m.put("y", b.y); m.put("z", b.z);
            m.put("state", b.state.name()); m.put("countdown", b.countdown); m.put("wave", b.wave);
            list.add(m);
        }
        y.set("bases", list);
        List<Map<String, Object>> tl = new ArrayList<>();
        for (Turret t : turrets.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("world", t.world); m.put("x", t.x); m.put("y", t.y); m.put("z", t.z);
            m.put("level", t.level); m.put("dirt", t.dirt);
            tl.add(m);
        }
        y.set("turrets", tl);
        try { y.save(dataFile); } catch (Exception ex) { getLogger().warning("Не удалось сохранить: " + ex.getMessage()); }
    }

    void load() {
        if (!dataFile.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(dataFile);
        for (Map<?, ?> m : y.getMapList("bases")) {
            try {
                Base b = new Base((String) m.get("world"), ((Number) m.get("x")).intValue(),
                        ((Number) m.get("y")).intValue(), ((Number) m.get("z")).intValue());
                b.state = State.valueOf((String) m.get("state"));
                b.countdown = ((Number) m.get("countdown")).intValue();
                b.wave = ((Number) m.get("wave")).intValue();
                bases.put(b.key(), b);
            } catch (Exception ignored) { }
        }
        for (Map<?, ?> m : y.getMapList("turrets")) {
            try {
                Turret t = new Turret((String) m.get("world"), ((Number) m.get("x")).intValue(),
                        ((Number) m.get("y")).intValue(), ((Number) m.get("z")).intValue());
                t.level = Math.max(1, Math.min(20, ((Number) m.get("level")).intValue()));
                if (m.get("dirt") instanceof Number n) t.dirt = Math.max(0, n.intValue());
                turrets.put(t.key(), t);
            } catch (Exception ignored) { }
        }
    }

    // ===== Утилиты =====
    String role(Entity e) { return e.getPersistentDataContainer().get(ROLE, PersistentDataType.STRING); }
    String baseOf(Entity e) { return e.getPersistentDataContainer().get(BASE, PersistentDataType.STRING); }
    String keyOf(Block b) { return b.getWorld().getName() + ";" + b.getX() + ";" + b.getY() + ";" + b.getZ(); }
    boolean isBase(Block b) { return bases.containsKey(keyOf(b)); }
    boolean isTurretBlock(Block b) { return turrets.containsKey(keyOf(b)); }
    boolean isAlly(Entity e) { return e != null && e.getPersistentDataContainer().has(ALLY, PersistentDataType.STRING); }
    boolean isEnemy(Entity e) { String r = role(e); return r != null && !r.equals("negotiator"); }
    boolean isMilitia(Entity e) { return e instanceof Villager && e.getPersistentDataContainer().has(DEF, PersistentDataType.STRING); }
    double clamp(double v) { return Math.max(0.0, Math.min(1.0, v)); }

    void say(String s) { Bukkit.broadcastMessage(ChatColor.RED + "[Raider] " + s); }

    String itemId(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(ITEM, PersistentDataType.STRING);
    }

    boolean isCore(ItemStack it) {
        return it != null && it.hasItemMeta() && it.getItemMeta().getPersistentDataContainer().has(CORE, PersistentDataType.BYTE);
    }

    boolean isDirt(Material m) {
        return m == Material.DIRT || m == Material.GRASS_BLOCK || m == Material.COARSE_DIRT
                || m == Material.PODZOL || m == Material.ROOTED_DIRT || m == Material.MYCELIUM;
    }

    int countDirt(Player p) {
        int n = 0;
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && isDirt(it.getType()) && !it.hasItemMeta()) n += it.getAmount();
        }
        return n;
    }

    boolean takeDirt(Player p, int n) {
        if (countDirt(p) < n) return false;
        int left = n;
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < 36 && left > 0; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && isDirt(it.getType()) && !it.hasItemMeta()) {
                int t = Math.min(left, it.getAmount());
                left -= t;
                if (it.getAmount() - t <= 0) inv.setItem(i, null);
                else { it.setAmount(it.getAmount() - t); inv.setItem(i, it); }
            }
        }
        return true;
    }

    void giveItem(Player p, ItemStack it) {
        Map<Integer, ItemStack> left = p.getInventory().addItem(it);
        for (ItemStack l : left.values()) p.getWorld().dropItemNaturally(p.getLocation(), l);
    }

    Player nearest(Location l, double r) {
        Player best = null; double bd = r * r;
        for (Player p : l.getWorld().getPlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR || p.getGameMode() == GameMode.CREATIVE) continue;
            double dd = p.getLocation().distanceSquared(l);
            if (dd < bd) { bd = dd; best = p; }
        }
        return best;
    }

    LivingEntity nearestIn(List<? extends LivingEntity> list, Location l, double r) {
        LivingEntity best = null; double bd = r * r;
        for (LivingEntity le : list) {
            if (le.isDead() || !le.isValid() || !le.getWorld().equals(l.getWorld())) continue;
            double dd = le.getLocation().distanceSquared(l);
            if (dd < bd) { bd = dd; best = le; }
        }
        return best;
    }

    boolean isFlyer(Entity e) {
        return e instanceof Phantom || e instanceof Vex || e instanceof Blaze || e instanceof Ghast;
    }

    boolean isFlyerType(EntityType t) {
        return t == EntityType.PHANTOM || t == EntityType.VEX || t == EntityType.BLAZE || t == EntityType.GHAST;
    }

    /** Воздушная цель для зениток. */
    boolean air(Entity e) {
        String r = role(e);
        if (r == null || r.equals("negotiator")) return false;
        if (isFlyer(e) || r.equals("kamikaze") || r.equals("bomber")) return true;
        return e instanceof LivingEntity le && le.hasPotionEffect(PotionEffectType.SLOW_FALLING) && !e.isOnGround();
    }

    Location spawnPoint(Base b, double up) {
        World w = b.w();
        int mn = minDist(), mx = maxDist();
        for (int i = 0; i < 10; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double dd = mn + rnd.nextDouble() * (mx - mn);
            int x = b.x + (int) Math.round(Math.cos(ang) * dd);
            int z = b.z + (int) Math.round(Math.sin(ang) * dd);
            int y = w.getHighestBlockYAt(x, z);
            if (w.getBlockAt(x, y, z).isLiquid()) continue;
            return new Location(w, x + 0.5, y + 1 + up, z + 0.5);
        }
        int x = b.x + mn;
        return new Location(w, x + 0.5, w.getHighestBlockYAt(x, b.z) + 1 + up, b.z + 0.5);
    }

    Base nearestBase(CommandSender s) {
        Base best = null; double bd = Double.MAX_VALUE;
        for (Base b : bases.values()) {
            if (b.w() == null) continue;
            if (s instanceof Player p) {
                if (!p.getWorld().equals(b.w())) continue;
                double dd = p.getLocation().distanceSquared(b.loc());
                if (dd < bd) { bd = dd; best = b; }
            } else return b;
        }
        return best;
    }

    Base baseNear(Location l, double r, boolean needWar) {
        Base best = null; double bd = r * r;
        for (Base b : bases.values()) {
            if (b.w() == null || !b.w().equals(l.getWorld())) continue;
            if (needWar && b.state != State.WAR) continue;
            double dd = b.loc().distanceSquared(l);
            if (dd < bd) { bd = dd; best = b; }
        }
        return best;
    }

    void setScale(LivingEntity e, double s) {
        AttributeInstance a = e.getAttribute(Attribute.SCALE);
        if (a != null) a.setBaseValue(s);
    }

    void setHealth(LivingEntity e, double hp) {
        AttributeInstance a = e.getAttribute(Attribute.MAX_HEALTH);
        if (a != null) { a.setBaseValue(hp); e.setHealth(hp); }
    }

    void mulAttr(LivingEntity e, Attribute at, double mult) {
        if (mult == 1.0) return;
        AttributeInstance a = e.getAttribute(at);
        if (a != null) a.setBaseValue(a.getBaseValue() * mult);
    }

    LivingEntity reg(LivingEntity e, String role, Base b) {
        var pdc = e.getPersistentDataContainer();
        pdc.set(ROLE, PersistentDataType.STRING, role);
        pdc.set(BASE, PersistentDataType.STRING, b.key());
        e.setRemoveWhenFarAway(false);
        e.setCanPickupItems(false);
        AttributeInstance fr = e.getAttribute(Attribute.FOLLOW_RANGE);
        if (fr != null) fr.setBaseValue(cd("enemy.follow-range"));
        e.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, false, false));
        if (!role.equals("negotiator")) {
            AttributeInstance hp = e.getAttribute(Attribute.MAX_HEALTH);
            double hm = cd("enemy.health-multiplier");
            if (hp != null && hm != 1.0) { hp.setBaseValue(hp.getBaseValue() * hm); e.setHealth(hp.getBaseValue()); }
            mulAttr(e, Attribute.MOVEMENT_SPEED, cd("enemy.speed-multiplier"));
            mulAttr(e, Attribute.ATTACK_DAMAGE, cd("enemy.damage-multiplier"));
        }
        mobs.add(e.getUniqueId());
        return e;
    }

    void rescan(Entity en) {
        String r = role(en);
        if (r != null) {
            if (r.equals("kamikaze")) bees.putIfAbsent(en.getUniqueId(), 0);
            else if (en instanceof LivingEntity) mobs.add(en.getUniqueId());
        }
        if (isMilitia(en)) defenders.add(en.getUniqueId());
        if (isAlly(en)) en.remove();
        if (en instanceof BlockDisplay && en.getPersistentDataContainer().has(MODEL, PersistentDataType.STRING)) en.remove();
    }

    void removeMobs(String key, String onlyRole) {
        for (Iterator<UUID> it = mobs.iterator(); it.hasNext(); ) {
            UUID id = it.next();
            Entity en = Bukkit.getEntity(id);
            if (en == null) continue;
            if (!key.equals(baseOf(en))) continue;
            if (onlyRole != null && !onlyRole.equals(role(en))) continue;
            en.remove(); it.remove(); stuck.remove(id);
        }
        if (onlyRole == null) {
            for (Iterator<UUID> it = bees.keySet().iterator(); it.hasNext(); ) {
                Entity en = Bukkit.getEntity(it.next());
                if (en != null && key.equals(baseOf(en))) {
                    for (Entity p : new ArrayList<>(en.getPassengers())) p.remove();
                    en.remove(); it.remove();
                }
            }
        }
    }

    int alive(String key) {
        int n = 0;
        for (UUID id : mobs) {
            Entity en = Bukkit.getEntity(id);
            if (en != null && key.equals(baseOf(en))) n++;
        }
        for (UUID id : bees.keySet()) {
            Entity en = Bukkit.getEntity(id);
            if (en != null && key.equals(baseOf(en))) n++;
        }
        return n;
    }

    // ===== Предметы =====
    ItemStack mk(Material m, int amt, String id, String name, String model, List<String> lore, Object... ench) {
        ItemStack it = new ItemStack(m, amt);
        ItemMeta im = it.getItemMeta();
        im.setDisplayName(name);
        if (lore != null) im.setLore(lore);
        if (model != null) im.setItemModel(NamespacedKey.minecraft(model));
        for (int i = 0; i + 1 < ench.length; i += 2) im.addEnchant((Enchantment) ench[i], (Integer) ench[i + 1], true);
        im.getPersistentDataContainer().set(ITEM, PersistentDataType.STRING, id);
        it.setItemMeta(im);
        return it;
    }

    ItemStack dye(ItemStack it, Color col) {
        ItemMeta im = it.getItemMeta();
        if (im instanceof LeatherArmorMeta lm) { lm.setColor(col); it.setItemMeta(lm); }
        return it;
    }

    ItemStack core(int n) {
        ItemStack it = mk(CORE_MAT, n, "core", ChatColor.DARK_PURPLE + "Ядро Рейдера",
                null, List.of(ChatColor.GRAY + "Его жаждут все враждебные мобы",
                        ChatColor.GRAY + "Поставь на землю: это твоя база.",
                        ChatColor.GRAY + "ПКМ по маяку: военный магазин (за землю)"));
        ItemMeta m = it.getItemMeta();
        m.setEnchantmentGlintOverride(true);
        m.getPersistentDataContainer().set(CORE, PersistentDataType.BYTE, (byte) 1);
        it.setItemMeta(m);
        return it;
    }

    ItemStack turretItem(int n, int level) {
        ItemStack it = mk(TURRET_MAT, n, "turret", ChatColor.AQUA + "Зенитка",
                null, List.of(ChatColor.GRAY + "Ставится отдельно от ядра.", ChatColor.GRAY + "Стреляет фаерболами по воздушным целям.",
                        ChatColor.GRAY + "ПКМ землёй по зенитке: улучшение до 20 уровня.",
                        ChatColor.YELLOW + "Уровень: " + level));
        ItemMeta m = it.getItemMeta();
        m.getPersistentDataContainer().set(TLEVEL, PersistentDataType.INTEGER, level);
        it.setItemMeta(m);
        return it;
    }

    ItemStack make(String id, int n) {
        switch (id) {
            case "core": return core(n);
            case "turret": return turretItem(n, 1);
            case "vest_light":
                return dye(mk(Material.LEATHER_CHESTPLATE, n, id, ChatColor.GREEN + "Бронежилет «Лёгкий»", null,
                        List.of(ChatColor.GRAY + "Лёгкая защита, не сковывает"),
                        Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3), Color.fromRGB(70, 95, 55));
            case "vest_heavy":
                return mk(Material.IRON_CHESTPLATE, n, id, ChatColor.GOLD + "Бронежилет «Тяжёлый»", null,
                        List.of(ChatColor.GRAY + "Хорошая защита от пуль и взрывов"),
                        Enchantment.PROTECTION, 5, Enchantment.BLAST_PROTECTION, 4, Enchantment.UNBREAKING, 3);
            case "vest_assault":
                return mk(Material.NETHERITE_CHESTPLATE, n, id, ChatColor.DARK_RED + "Бронежилет «Штурмовой»", null,
                        List.of(ChatColor.GRAY + "Лучшая защита в магазине"),
                        Enchantment.PROTECTION, 7, Enchantment.BLAST_PROTECTION, 6, Enchantment.PROJECTILE_PROTECTION, 6,
                        Enchantment.THORNS, 2, Enchantment.UNBREAKING, 4);
            case "helmet":
                return dye(mk(Material.LEATHER_HELMET, n, id, ChatColor.GREEN + "Каска", null,
                        List.of(ChatColor.GRAY + "Защита головы"), Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3),
                        Color.fromRGB(70, 95, 55));
            case "boots":
                return dye(mk(Material.LEATHER_BOOTS, n, id, ChatColor.GREEN + "Берцы", null,
                        List.of(ChatColor.GRAY + "Прочные армейские ботинки"),
                        Enchantment.PROTECTION, 4, Enchantment.FEATHER_FALLING, 4, Enchantment.UNBREAKING, 3),
                        Color.fromRGB(35, 35, 35));
            case "rifle":
                return mk(Material.IRON_HOE, n, id, ChatColor.YELLOW + "Автомат", "crossbow",
                        List.of(ChatColor.GRAY + "Зажми ПКМ: автоматический огонь", ChatColor.GRAY + "Нужны патроны"));
            case "mg":
                return mk(Material.GOLDEN_HOE, n, id, ChatColor.GOLD + "Пулемёт", "crossbow",
                        List.of(ChatColor.GRAY + "Зажми ПКМ: плотный огонь", ChatColor.GRAY + "Нужны патроны"),
                        Enchantment.UNBREAKING, 1);
            case "sniper":
                return mk(Material.NETHERITE_HOE, n, id, ChatColor.DARK_GREEN + "Снайперская винтовка", "spyglass",
                        List.of(ChatColor.GRAY + "ПКМ: мощный дальний выстрел", ChatColor.GRAY + "Нужны патроны"));
            case "ammo":
                return mk(Material.ARROW, n, id, ChatColor.WHITE + "Патроны", null,
                        List.of(ChatColor.GRAY + "Расходуются при стрельбе"));
            case "grenade":
                return mk(Material.SNOWBALL, n, id, ChatColor.DARK_GREEN + "Граната осколочная", "fire_charge",
                        List.of(ChatColor.GRAY + "Брось: взрыв без разрушения блоков"));
            case "grenade_fire":
                return mk(Material.SNOWBALL, n, id, ChatColor.RED + "Граната зажигательная", "magma_cream",
                        List.of(ChatColor.GRAY + "Брось: огонь на месте падения"));
            case "shovel_iron":
                return mk(Material.IRON_SHOVEL, n, id, ChatColor.WHITE + "Лопата «Копатель»", null,
                        List.of(ChatColor.GRAY + "Больше земли с каждого блока"),
                        Enchantment.EFFICIENCY, 4, Enchantment.UNBREAKING, 3);
            case "shovel_diamond":
                return mk(Material.DIAMOND_SHOVEL, n, id, ChatColor.AQUA + "Лопата «Землекоп»", null,
                        List.of(ChatColor.GRAY + "Ещё больше земли"),
                        Enchantment.EFFICIENCY, 5, Enchantment.UNBREAKING, 4);
            case "shovel_netherite":
                return mk(Material.NETHERITE_SHOVEL, n, id, ChatColor.DARK_PURPLE + "Лопата «Экскаватор»", null,
                        List.of(ChatColor.GRAY + "Максимум земли"),
                        Enchantment.EFFICIENCY, 7, Enchantment.UNBREAKING, 5);
            case "radio":
                return mk(Material.COMPASS, n, id, ChatColor.BLUE + "Рация", "recovery_compass",
                        List.of(ChatColor.GRAY + "ПКМ: вызвать синих союзников", ChatColor.GRAY + "Перезарядка есть"));
            case "nuke":
                return mk(Material.TRIPWIRE_HOOK, n, id, ChatColor.DARK_RED + "" + ChatColor.BOLD + "Пульт ядерной бомбы", "tnt",
                        List.of(ChatColor.RED + "ПКМ: на месте ближайшего маяка", ChatColor.RED + "появится огромная воронка.",
                                ChatColor.RED + "Все враги погибнут. База тоже!"));
            default: return null;
        }
    }

    // ===== Меню: магазин и штаб ополчения =====
    ItemStack icon(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta im = it.getItemMeta();
        im.setDisplayName(name);
        if (lore.length > 0) im.setLore(Arrays.asList(lore));
        it.setItemMeta(im);
        return it;
    }

    void fillGlass(Inventory inv) {
        ItemStack g = icon(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, g);
    }

    void openShop(Player p) {
        Menu m = new Menu("shop", null);
        Inventory inv = Bukkit.createInventory(m, 54, ChatColor.DARK_RED + "Военный магазин (за землю)");
        m.inv = inv;
        renderShop(inv, p);
        p.openInventory(inv);
    }

    void renderShop(Inventory inv, Player p) {
        fillGlass(inv);
        for (String[] e : SHOP) {
            ItemStack it = make(e[0], Integer.parseInt(e[2]));
            ItemMeta im = it.getItemMeta();
            List<String> lore = im.getLore() == null ? new ArrayList<>() : new ArrayList<>(im.getLore());
            lore.add("");
            lore.add(ChatColor.GOLD + "Цена: " + ci("prices." + e[0]) + " земли" + (Integer.parseInt(e[2]) > 1 ? " (за " + e[2] + " шт.)" : ""));
            lore.add(ChatColor.YELLOW + "Клик: купить");
            im.setLore(lore);
            it.setItemMeta(im);
            inv.setItem(Integer.parseInt(e[1]), it);
        }
        inv.setItem(49, icon(Material.DIRT, ChatColor.GOLD + "Твоя земля: " + countDirt(p),
                ChatColor.GRAY + "Земля из инвентаря — деньги", ChatColor.GRAY + "Копай лопатой!"));
    }

    void buy(Player p, String id, int amount, Inventory inv) {
        int price = ci("prices." + id);
        if (!takeDirt(p, price)) {
            p.sendMessage(ChatColor.RED + "Не хватает земли: нужно " + price + ", у тебя " + countDirt(p));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        giveItem(p, make(id, amount));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1f, 1f);
        renderShop(inv, p);
    }

    int vi(Entity v, NamespacedKey k) {
        Integer x = v.getPersistentDataContainer().get(k, PersistentDataType.INTEGER);
        return x == null ? 0 : x;
    }

    void vset(Entity v, NamespacedKey k, int val) { v.getPersistentDataContainer().set(k, PersistentDataType.INTEGER, val); }

    void openMilitia(Player p, Villager v) {
        Menu m = new Menu("militia", v.getUniqueId());
        Inventory inv = Bukkit.createInventory(m, 27, ChatColor.DARK_GREEN + "Штаб ополчения");
        m.inv = inv;
        renderMilitia(inv, p, v);
        p.openInventory(inv);
    }

    void renderMilitia(Inventory inv, Player p, Villager v) {
        fillGlass(inv);
        boolean en = isMilitia(v);
        if (!en) {
            inv.setItem(10, icon(Material.EMERALD, ChatColor.GREEN + "Записать в ополчение",
                    ChatColor.GOLD + "Цена: " + ci("militia.enlist-cost") + " земли",
                    ChatColor.GRAY + "Ополченцы защищают базу и чинят стены"));
        } else {
            int w = vi(v, V_WEAPON), a = vi(v, V_ARMOR), mx = ci("militia.skill-max");
            inv.setItem(10, icon(Material.VILLAGER_SPAWN_EGG, ChatColor.GREEN + "Ополченец",
                    ChatColor.GRAY + "Оружие: " + WNAMES[w], ChatColor.GRAY + "Броня: ур. " + a + "/3"));
            inv.setItem(11, w >= 4 ? icon(Material.CROSSBOW, ChatColor.GOLD + "Оружие: " + WNAMES[w], ChatColor.GRAY + "Максимум")
                    : icon(Material.CROSSBOW, ChatColor.GOLD + "Оружие: " + WNAMES[w], ChatColor.YELLOW + "Следующее: " + WNAMES[w + 1],
                    ChatColor.GOLD + "Цена: " + ci("militia.weapon-cost") * (w + 1) + " земли"));
            inv.setItem(12, a >= 3 ? icon(Material.IRON_CHESTPLATE, ChatColor.GOLD + "Броня: ур. " + a, ChatColor.GRAY + "Максимум")
                    : icon(Material.IRON_CHESTPLATE, ChatColor.GOLD + "Броня: ур. " + a, ChatColor.YELLOW + "Улучшить до " + (a + 1),
                    ChatColor.GOLD + "Цена: " + ci("militia.armor-cost") * (a + 1) + " земли"));
            Material[] ic = {Material.SPYGLASS, Material.CLOCK, Material.GOLDEN_APPLE, Material.DIAMOND_PICKAXE};
            NamespacedKey[] ks = {V_AIM, V_RATE, V_HP, V_REPAIR};
            for (int i = 0; i < 4; i++) {
                int lv = vi(v, ks[i]);
                inv.setItem(13 + i, lv >= mx ? icon(ic[i], ChatColor.GOLD + SKILLS[i] + ": " + lv + "/" + mx, ChatColor.GRAY + "Максимум")
                        : icon(ic[i], ChatColor.GOLD + SKILLS[i] + ": " + lv + "/" + mx, ChatColor.YELLOW + "Обучить",
                        ChatColor.GOLD + "Цена: " + ci("militia.skill-cost") * (lv + 1) + " земли"));
            }
        }
        inv.setItem(22, icon(Material.DIRT, ChatColor.GOLD + "Твоя земля: " + countDirt(p)));
    }

    void enlist(Villager v) {
        v.getPersistentDataContainer().set(DEF, PersistentDataType.STRING, "1");
        defenders.add(v.getUniqueId());
        refreshVillager(v);
    }

    ItemStack weaponDisplay(int w) {
        return switch (w) {
            case 1 -> new ItemStack(Material.BOW);
            case 2 -> make("rifle", 1);
            case 3 -> make("mg", 1);
            case 4 -> new ItemStack(Material.FIRE_CHARGE);
            default -> new ItemStack(Material.SNOWBALL);
        };
    }

    void refreshVillager(Villager v) {
        int w = vi(v, V_WEAPON), a = vi(v, V_ARMOR);
        AttributeInstance hp = v.getAttribute(Attribute.MAX_HEALTH);
        double mh = 20 + cd("villagers.hp-per-level") * vi(v, V_HP);
        if (hp != null) { hp.setBaseValue(mh); v.setHealth(Math.min(mh, Math.max(v.getHealth(), 1))); }
        v.getEquipment().setItemInMainHand(weaponDisplay(w));
        v.setCustomName(ChatColor.GREEN + "Ополченец " + ChatColor.GRAY + "[" + WNAMES[w] + " | бр." + a + "]");
        v.setCustomNameVisible(true);
    }

    void militiaClick(Player p, Menu m, int slot, Inventory inv) {
        Entity en = Bukkit.getEntity(m.target);
        if (!(en instanceof Villager v) || v.isDead()) { p.closeInventory(); return; }
        if (!isMilitia(v)) {
            if (slot == 10) {
                if (!takeDirt(p, ci("militia.enlist-cost"))) { noMoney(p); return; }
                enlist(v);
                emote(v, "enlist", 1.0);
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
            }
            renderMilitia(inv, p, v);
            return;
        }
        boolean did = false; String ek = "upgrade";
        if (slot == 11) {
            int w = vi(v, V_WEAPON);
            if (w < 4 && takeDirt(p, ci("militia.weapon-cost") * (w + 1))) { vset(v, V_WEAPON, w + 1); did = true; ek = "armed"; }
            else if (w < 4) { noMoney(p); return; }
        } else if (slot == 12) {
            int a = vi(v, V_ARMOR);
            if (a < 3 && takeDirt(p, ci("militia.armor-cost") * (a + 1))) { vset(v, V_ARMOR, a + 1); did = true; }
            else if (a < 3) { noMoney(p); return; }
        } else if (slot >= 13 && slot <= 16) {
            NamespacedKey[] ks = {V_AIM, V_RATE, V_HP, V_REPAIR};
            NamespacedKey k = ks[slot - 13];
            int lv = vi(v, k);
            if (lv < ci("militia.skill-max") && takeDirt(p, ci("militia.skill-cost") * (lv + 1))) { vset(v, k, lv + 1); did = true; }
            else if (lv < ci("militia.skill-max")) { noMoney(p); return; }
        }
        if (did) {
            refreshVillager(v);
            emote(v, ek, 1.0);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            v.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, v.getLocation().add(0, 1.5, 0), 15, 0.4, 0.5, 0.4, 0.1);
        }
        renderMilitia(inv, p, v);
    }

    void noMoney(Player p) {
        p.sendMessage(ChatColor.RED + "Не хватает земли! У тебя " + countDirt(p));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }

    // ===== Логика баз (раз в секунду) =====
    void secondTick() {
        boolean dirty = false;
        for (Base b : new ArrayList<>(bases.values())) {
            World w = b.w();
            if (w == null || !w.isChunkLoaded(b.x >> 4, b.z >> 4)) continue;
            if (b.loc().getBlock().getType() != CORE_MAT) {
                dropBase(b); dirty = true; continue;
            }
            b.alive = alive(b.key());
            switch (b.state) {
                case WAIT -> {
                    if (--b.countdown <= 0) {
                        b.state = State.NEGOTIATE; b.countdown = ci("timing.negotiate-seconds");
                        spawnNegotiators(b); dirty = true;
                    }
                }
                case NEGOTIATE -> {
                    if (--b.countdown <= 0) { startWar(b, "Время вышло! Они не получили Ядро и идут за ним силой!"); dirty = true; }
                }
                case WAR -> {
                    if (!b.cleared && !b.spawning && b.alive == 0 && b.wave > 0) {
                        b.cleared = true;
                        emoteAround(b.loc(), 80, "wavedone", 0.6);
                    }
                    if (--b.countdown <= 0) {
                        b.wave++; b.countdown = ci("timing.wave-seconds");
                        spawnWave(b); dirty = true;
                    }
                }
                default -> { }
            }
            updateBar(b);
        }
        if (dirty) save();
        if (rnd.nextInt(40) == 0) {
            for (UUID id : defenders) {
                Entity en = Bukkit.getEntity(id);
                if (en instanceof Villager v && baseNear(v.getLocation(), 80, true) == null && rnd.nextInt(6) == 0) { emote(v, "idle", 1.0); break; }
            }
        }
    }

    void dropBase(Base b) {
        bases.remove(b.key());
        removeMobs(b.key(), null);
        if (b.bar != null) { b.bar.removeAll(); b.bar = null; }
    }

    void startWar(Base b, String msg) {
        b.state = State.WAR; b.countdown = 5;
        for (UUID id : mobs) {
            Entity en = Bukkit.getEntity(id);
            if (en != null && b.key().equals(baseOf(en)) && "negotiator".equals(role(en)))
                en.getPersistentDataContainer().set(ROLE, PersistentDataType.STRING, "raider");
        }
        say(msg);
    }

    void spawnNegotiators(Base b) {
        int n = 3 + rnd.nextInt(3);
        for (int i = 0; i < n; i++) {
            Location l = spawnPoint(b, 0);
            Entity ent = b.w().spawnEntity(l, EntityType.PILLAGER);
            if (!(ent instanceof Pillager p)) { ent.remove(); continue; }
            p.getEquipment().setItemInMainHand(null);
            p.setCustomName(ChatColor.GOLD + (i == 0 ? "Вожак переговорщиков" : "Переговорщик"));
            p.setCustomNameVisible(true);
            setHealth(p, 40);
            reg(p, "negotiator", b);
        }
        say("К базе " + b.x + " " + b.y + " " + b.z + " идёт группа мобов. Они требуют Ядро Рейдера! "
                + "Отдайте его (ПКМ ядром по переговорщику) за " + ci("timing.negotiate-seconds") + " сек. Если нападёте - начнётся война.");
    }

    // ===== Боссбар волны =====
    void updateBar(Base b) {
        World w = b.w();
        if (w == null) return;
        if (!cb("bar.enabled") || (b.state != State.WAR && b.state != State.NEGOTIATE)) {
            if (b.bar != null) { b.bar.removeAll(); b.bar = null; }
            return;
        }
        if (b.bar == null) b.bar = Bukkit.createBossBar("", BarColor.RED, BarStyle.SEGMENTED_10);
        BossBar bar = b.bar;
        if (b.state == State.NEGOTIATE) {
            bar.setColor(BarColor.YELLOW);
            bar.setTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "Переговоры: осталось " + b.countdown + " с");
            bar.setProgress(clamp(b.countdown / (double) Math.max(1, ci("timing.negotiate-seconds"))));
        } else {
            bar.setColor(BarColor.RED);
            if (b.wave == 0) {
                bar.setTitle(ChatColor.RED + "" + ChatColor.BOLD + "Война начинается...");
                bar.setProgress(1.0);
            } else {
                bar.setTitle(ChatColor.RED + "" + ChatColor.BOLD + "Волна " + b.wave + " | врагов: " + b.alive
                        + " | следующая через " + b.countdown + " с");
                bar.setProgress(b.waveTotal > 0 ? clamp(b.alive / (double) b.waveTotal) : 0.0);
            }
        }
        double r = cd("bar.radius");
        Location bl = b.loc();
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distanceSquared(bl) <= r * r) bar.addPlayer(p); else bar.removePlayer(p);
        }
    }

    // ===== Сирена воздушной тревоги: протяжный вой вверх-вниз =====
    void siren() {
        if (!cb("siren.enabled")) return;
        double r = cd("siren.radius");
        double cycle = Math.max(2.0, cd("siren.cycle-seconds"));
        double mn = Math.max(0.5, cd("siren.min-pitch")), mx = Math.min(2.0, cd("siren.max-pitch"));
        double s = 0.5 - 0.5 * Math.cos(2 * Math.PI * (c * 0.1) / cycle);
        float pitch = (float) (mn + (mx - mn) * s);
        float vol = (float) cd("siren.volume");
        for (Base b : bases.values()) {
            World w = b.w();
            if (w == null || b.state != State.WAR || b.alive <= 0) continue;
            Location bl = b.loc();
            for (Player p : w.getPlayers()) {
                if (p.getLocation().distanceSquared(bl) > r * r) continue;
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, SoundCategory.MASTER, vol, pitch);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, SoundCategory.MASTER, vol * 0.8f, pitch * 0.5f);
            }
        }
    }

    // ===== Волны =====
    EntityType pickType(int wave) {
        int max = Math.min(TIERS.length - 1, (wave - 1) / 2);
        int idx = rnd.nextInt(3) == 0 ? max : rnd.nextInt(max + 1);
        EntityType[] t = TIERS[idx];
        return t[rnd.nextInt(t.length)];
    }

    void spawnWave(Base b) {
        int wave = b.wave;
        double mult = cd("attackers.multiplier");
        int want = (int) Math.round((ci("attackers.base-count") + ci("attackers.per-wave") * (wave - 1)) * mult);
        want = Math.min(want, ci("attackers.max-per-wave"));
        int room = ci("attackers.max-alive") - alive(b.key());
        int n = Math.max(0, Math.min(want, room));
        int kam = 0;
        if (wave >= 2) {
            kam = (int) Math.round((ci("attackers.kamikaze-base") + ci("attackers.kamikaze-per-wave") * (wave - 1)) * mult);
            kam = Math.min(kam, ci("attackers.kamikaze-max"));
        }
        b.waveTotal = Math.max(1, n + kam);
        b.spawning = true;
        b.cleared = false;
        bombersThisWave = 0;
        final int total = n, kamTotal = kam;
        final int batch = Math.max(1, ci("attackers.spawn-batch"));
        new BukkitRunnable() {
            int left = total, kleft = kamTotal;
            @Override public void run() {
                if (!bases.containsKey(b.key()) || b.w() == null) { b.spawning = false; cancel(); return; }
                for (int i = 0; i < batch && left > 0; i++, left--) spawnRaider(b);
                for (int i = 0; i < Math.max(1, batch / 2) && kleft > 0; i++, kleft--) {
                    boolean drone = b.wave >= ci("specials.drone-from-wave") && rnd.nextBoolean();
                    spawnKamikaze(b, drone ? EntityType.VEX : EntityType.BEE);
                }
                if (left <= 0 && kleft <= 0) { b.spawning = false; cancel(); }
            }
        }.runTaskTimer(this, 0L, 2L);
        recruit(b);
        emoteAround(b.loc(), 80, "wavestart", 0.7);
        String extra = "";
        if (wave == ci("specials.gunner-from-wave")) extra = " Появились автоматчики!";
        else if (wave == ci("specials.drone-from-wave")) extra = " Дроны-смертники в небе!";
        else if (wave == ci("specials.bomber-from-wave")) extra = " Бомбардировщики и снайперы!";
        else if (wave == ci("specials.tank-from-wave")) extra = " Появились танки!";
        else if (wave == ci("specials.para-from-wave")) extra = " Десант с неба!";
        else if (wave == ci("specials.mortar-from-wave")) extra = " Появились миномёты!";
        say("Волна " + wave + " атакует базу! Врагов: " + b.waveTotal + "." + extra);
    }

    void spawnRaider(Base b) {
        if (!bases.containsKey(b.key()) || b.w() == null) return;
        int wave = b.wave;
        if (rnd.nextDouble() < cd("attackers.special-chance")) {
            List<String> sp = new ArrayList<>();
            if (wave >= ci("specials.gunner-from-wave")) sp.add("gunner");
            if (wave >= ci("specials.sniper-from-wave")) sp.add("sniper");
            if (wave >= ci("specials.bomber-from-wave") && bombersThisWave < ci("specials.max-bombers")) sp.add("bomber");
            if (wave >= ci("specials.tank-from-wave")) sp.add("tank");
            if (wave >= ci("specials.para-from-wave")) sp.add("para");
            if (wave >= ci("specials.mortar-from-wave")) sp.add("mortar");
            if (!sp.isEmpty()) {
                String pick = sp.get(rnd.nextInt(sp.size()));
                if (pick.equals("bomber")) bombersThisWave++;
                spawnRole(b, pick);
                return;
            }
        }
        EntityType t = pickType(wave);
        Location l = spawnPoint(b, isFlyerType(t) ? 10 : 0);
        Entity ent = b.w().spawnEntity(l, t);
        if (!(ent instanceof LivingEntity e)) { ent.remove(); return; }
        reg(e, "raider", b);
        mutate(e, t, wave);
        if (wave >= ci("enemy.riders-from-wave") && rnd.nextInt(5) == 0) {
            EntityType t2 = pickType(wave);
            Entity second = b.w().spawnEntity(l, t2);
            if (second instanceof LivingEntity e2) {
                reg(e2, "raider", b);
                mutate(e2, t2, wave);
                e.addPassenger(e2);
            } else second.remove();
        }
    }

    void mutate(LivingEntity e, EntityType t, int wave) {
        if (!cb("enemy.mutations")) return;
        if (wave >= 3 && e instanceof Zombie z && rnd.nextInt(5) == 0) z.setBaby(true);
        if (wave >= 4 && (e instanceof Zombie || e instanceof Skeleton) && e.getEquipment() != null && rnd.nextInt(3) == 0) {
            e.getEquipment().setHelmet(new ItemStack(wave >= 9 ? Material.DIAMOND_HELMET : Material.IRON_HELMET));
            e.getEquipment().setHelmetDropChance(0f);
        }
        if (wave >= 5 && rnd.nextInt(4) == 0) setScale(e, 0.5 + rnd.nextDouble() * (wave >= 10 ? 2.5 : 1.2));
        if (e instanceof Creeper cr && wave >= 6 && rnd.nextInt(3) == 0) cr.setPowered(true);
        if (wave >= 6 && rnd.nextInt(4) == 0) e.setGlowing(true);
        if (wave >= 8 && rnd.nextInt(3) == 0) {
            PotionEffectType[] fx = {PotionEffectType.SPEED, PotionEffectType.STRENGTH, PotionEffectType.RESISTANCE, PotionEffectType.REGENERATION};
            e.addPotionEffect(new PotionEffect(fx[rnd.nextInt(fx.length)], PotionEffect.INFINITE_DURATION, 1, false, false));
        }
        if (wave >= 8 && rnd.nextInt(4) == 0) {
            e.setCustomName(ChatColor.DARK_RED + STRANGE[rnd.nextInt(STRANGE.length)] + " " + t.name().toLowerCase().replace('_', ' '));
        }
    }

    void clearGear(LivingEntity e) {
        var eq = e.getEquipment();
        if (eq == null) return;
        eq.setHelmet(null); eq.setChestplate(null); eq.setLeggings(null); eq.setBoots(null);
        eq.setItemInMainHand(null); eq.setItemInOffHand(null);
    }

    void spawnRole(Base b, String role) {
        if (!bases.containsKey(b.key()) || b.w() == null) return;
        World w = b.w();
        switch (role) {
            case "gunner", "sniper" -> {
                Entity ent = w.spawnEntity(spawnPoint(b, 0), EntityType.SKELETON);
                if (!(ent instanceof Skeleton s)) { ent.remove(); return; }
                var eq = s.getEquipment();
                eq.setItemInMainHand(null);
                eq.setHelmet(new ItemStack(Material.IRON_HELMET));
                eq.setHelmetDropChance(0f);
                if (role.equals("gunner")) {
                    eq.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                    s.setCustomName(ChatColor.GRAY + "Автоматчик");
                } else {
                    eq.setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
                    s.setCustomName(ChatColor.DARK_GREEN + "Снайпер");
                }
                eq.setChestplateDropChance(0f);
                reg(s, role, b);
                attachModel(s, role);
            }
            case "tank" -> {
                Entity ent = w.spawnEntity(spawnPoint(b, 0), EntityType.RAVAGER);
                if (!(ent instanceof Ravager rv)) { ent.remove(); return; }
                setScale(rv, 1.5);
                setHealth(rv, ci("specials.tank-health"));
                rv.setCustomName(ChatColor.DARK_GREEN + "Танк");
                rv.setInvisible(true);
                reg(rv, "tank", b);
                attachModel(rv, "tank");
                emoteAround(b.loc(), 80, "tank", 0.5);
            }
            case "mortar" -> {
                Entity ent = w.spawnEntity(spawnPoint(b, 0), EntityType.ZOMBIE);
                if (!(ent instanceof Zombie z)) { ent.remove(); return; }
                clearGear(z);
                z.setBaby(false);
                z.setCustomName(ChatColor.RED + "Миномёт");
                z.setInvisible(true);
                reg(z, "mortar", b);
                attachModel(z, "mortar");
            }
            case "bomber" -> {
                Entity ent = w.spawnEntity(spawnPoint(b, 22), EntityType.PHANTOM);
                if (!(ent instanceof Phantom ph)) { ent.remove(); return; }
                setScale(ph, 3.0);
                setHealth(ph, ci("specials.bomber-health"));
                ph.setAware(false);
                ph.setGravity(false);
                ph.setInvisible(true);
                ph.setCustomName(ChatColor.DARK_GRAY + "Бомбардировщик");
                reg(ph, "bomber", b);
                attachModel(ph, "bomber");
                emoteAround(b.loc(), 80, "bomber", 0.5);
            }
            case "para" -> spawnParatrooper(b);
            default -> { }
        }
    }

    void spawnParatrooper(Base b) {
        EntityType t = pickType(b.wave);
        if (isFlyerType(t) || t == EntityType.SLIME || t == EntityType.MAGMA_CUBE) t = EntityType.ZOMBIE;
        Location l = spawnPoint(b, 45);
        Entity ent = b.w().spawnEntity(l, t);
        if (!(ent instanceof LivingEntity e)) { ent.remove(); return; }
        reg(e, "para", b);
        mutate(e, t, b.wave);
        e.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 40, 0, false, false));
        attachModel(e, "para");
    }

    void spawnKamikaze(Base b, EntityType t) {
        if (!bases.containsKey(b.key()) || b.w() == null) return;
        Location l = spawnPoint(b, 8);
        Entity ent = b.w().spawnEntity(l, t);
        if (!(ent instanceof Mob m)) { ent.remove(); return; }
        m.setRemoveWhenFarAway(false);
        m.setAware(false);
        m.setGravity(false);
        var pdc = m.getPersistentDataContainer();
        pdc.set(ROLE, PersistentDataType.STRING, "kamikaze");
        pdc.set(BASE, PersistentDataType.STRING, b.key());
        if (m instanceof Vex vx) {
            vx.setCharging(false);
            setScale(vx, 1.5);
            vx.setInvisible(true);
            vx.setCustomName(ChatColor.AQUA + "Дрон-смертник");
            clearGear(vx);
            attachModel(vx, "drone");
        } else {
            m.setCustomName(ChatColor.YELLOW + "Пчела-смертник");
            Entity tnt = b.w().spawnEntity(l, EntityType.TNT);
            if (tnt instanceof TNTPrimed tp) { tp.setFuseTicks(32767); m.addPassenger(tp); }
        }
        bees.put(m.getUniqueId(), 0);
    }

    // ===== Модели из блок-дисплеев =====
    BlockDisplay bd(Location at, Material m, String tag, double tx, double ty, double tz,
                    double sx, double sy, double sz, Quaternionf rot) {
        BlockDisplay d = (BlockDisplay) at.getWorld().spawnEntity(at, EntityType.BLOCK_DISPLAY);
        d.setBlock(m.createBlockData());
        d.setTransformation(new Transformation(new Vector3f((float) tx, (float) ty, (float) tz),
                rot == null ? new Quaternionf() : rot, new Vector3f((float) sx, (float) sy, (float) sz), new Quaternionf()));
        d.setPersistent(false);
        d.setTeleportDuration(2);
        d.setViewRange(3f);
        d.getPersistentDataContainer().set(MODEL, PersistentDataType.STRING, tag);
        return d;
    }

    void attachModel(LivingEntity e, String kind) {
        if (models.containsKey(e.getUniqueId())) return;
        Mdl md = new Mdl();
        md.kind = kind;
        Location at = e.getLocation();
        String tag = "v:" + e.getUniqueId();
        List<BlockDisplay> p = md.parts;
        switch (kind) {
            case "tank" -> {
                p.add(bd(at, Material.BLACK_CONCRETE, tag, -1.45, 0.0, -1.7, 0.55, 0.75, 3.4, null));
                p.add(bd(at, Material.BLACK_CONCRETE, tag, 0.9, 0.0, -1.7, 0.55, 0.75, 3.4, null));
                for (int i = 0; i < 4; i++) {
                    double z = -1.4 + i * 0.95;
                    p.add(bd(at, Material.GRAY_CONCRETE, tag, -1.55, 0.1, z, 0.12, 0.5, 0.5, null));
                    p.add(bd(at, Material.GRAY_CONCRETE, tag, 1.43, 0.1, z, 0.12, 0.5, 0.5, null));
                }
                p.add(bd(at, Material.GREEN_TERRACOTTA, tag, -0.95, 0.35, -1.6, 1.9, 0.7, 3.2, null));
                p.add(bd(at, Material.GREEN_TERRACOTTA, tag, -0.8, 1.05, -1.4, 1.6, 0.3, 2.3, null));
                p.add(bd(at, Material.GREEN_TERRACOTTA, tag, -0.7, 1.35, -0.9, 1.4, 0.5, 1.6, null));
                p.add(bd(at, Material.GRAY_CONCRETE, tag, -0.2, 1.85, -0.5, 0.45, 0.12, 0.45, null));
                p.add(bd(at, Material.GRAY_CONCRETE, tag, -0.11, 1.5, 0.7, 0.22, 0.22, 2.4, null));
                p.add(bd(at, Material.BLACK_CONCRETE, tag, -0.16, 1.47, 3.0, 0.32, 0.28, 0.3, null));
                p.add(bd(at, Material.GRAY_CONCRETE, tag, 0.45, 1.9, -0.9, 0.08, 0.5, 0.08, null));
            }
            case "bomber" -> {
                p.add(bd(at, Material.LIGHT_GRAY_CONCRETE, tag, -0.3, -0.2, -1.8, 0.6, 0.6, 3.6, null));
                p.add(bd(at, Material.GRAY_CONCRETE, tag, -0.22, -0.15, 1.8, 0.44, 0.44, 0.5, null));
                p.add(bd(at, Material.LIGHT_BLUE_STAINED_GLASS, tag, -0.2, 0.4, 0.3, 0.4, 0.22, 0.8, null));
                p.add(bd(at, Material.GRAY_CONCRETE, tag, -2.9, -0.05, -0.5, 5.8, 0.12, 1.2, null));
                p.add(bd(at, Material.RED_CONCRETE, tag, -2.7, -0.04, -0.4, 0.4, 0.14, 0.4, null));
                p.add(bd(at, Material.RED_CONCRETE, tag, 2.3, -0.04, -0.4, 0.4, 0.14, 0.4, null));
                p.add(bd(at, Material.LIGHT_GRAY_CONCRETE, tag, -1.3, 0.1, -1.8, 2.6, 0.1, 0.6, null));
                p.add(bd(at, Material.RED_CONCRETE, tag, -0.06, 0.2, -1.8, 0.12, 0.8, 0.7, null));
                p.add(bd(at, Material.BLACK_CONCRETE, tag, -0.7, -0.15, 2.3, 1.4, 0.12, 0.08, null));
                p.add(bd(at, Material.BLACK_CONCRETE, tag, -1.5, -0.4, -0.3, 0.35, 0.35, 0.8, null));
                p.add(bd(at, Material.BLACK_CONCRETE, tag, 1.15, -0.4, -0.3, 0.35, 0.35, 0.8, null));
                p.add(bd(at, Material.BLACK_CONCRETE, tag, -0.18, -0.55, -0.4, 0.36, 0.3, 0.8, null));
            }
            case "mortar" -> {
                p.add(bd(at, Material.BLACK_CONCRETE, tag, -0.6, 0.0, -0.6, 1.2, 0.12, 1.2, null));
                p.add(bd(at, Material.GRAY_CONCRETE, tag, -0.12, 0.12, -0.2, 0.26, 1.5, 0.26, new Quaternionf().rotateX(0.35f)));
                p.add(bd(at, Material.BLACK_CONCRETE, tag, -0.5, 0.12, -0.1, 0.1, 0.7, 0.1, null));
                p.add(bd(at, Material.BLACK_CONCRETE, tag, 0.4, 0.12, -0.1, 0.1, 0.7, 0.1, null));
                p.add(bd(at, Material.TNT, tag, 0.45, 0.12, 0.4, 0.3, 0.3, 0.3, null));
                p.add(bd(at, Material.TNT, tag, -0.75, 0.12, 0.4, 0.3, 0.3, 0.3, null));
            }
            case "drone" -> {
                p.add(bd(at, Material.BLACK_CONCRETE, tag, -0.22, 0.5, -0.22, 0.44, 0.16, 0.44, null));
                p.add(bd(at, Material.GRAY_CONCRETE, tag, -0.6, 0.56, -0.04, 1.2, 0.05, 0.08, null));
                p.add(bd(at, Material.GRAY_CONCRETE, tag, -0.04, 0.56, -0.6, 0.08, 0.05, 1.2, null));
                p.add(bd(at, Material.GRAY_STAINED_GLASS, tag, -0.75, 0.62, -0.15, 0.3, 0.03, 0.3, null));
                p.add(bd(at, Material.GRAY_STAINED_GLASS, tag, 0.45, 0.62, -0.15, 0.3, 0.03, 0.3, null));
                p.add(bd(at, Material.GRAY_STAINED_GLASS, tag, -0.15, 0.62, -0.75, 0.3, 0.03, 0.3, null));
                p.add(bd(at, Material.GRAY_STAINED_GLASS, tag, -0.15, 0.62, 0.45, 0.3, 0.03, 0.3, null));
                p.add(bd(at, Material.TNT, tag, -0.15, 0.22, -0.15, 0.3, 0.3, 0.3, null));
                p.add(bd(at, Material.RED_CONCRETE, tag, -0.06, 0.52, 0.22, 0.12, 0.1, 0.1, null));
            }
            case "gunner" -> {
                p.add(bd(at, Material.BLACK_CONCRETE, tag, -0.28, 0.95, 0.15, 0.1, 0.14, 0.55, null));
                p.add(bd(at, Material.GRAY_CONCRETE, tag, -0.26, 1.0, 0.65, 0.06, 0.06, 0.5, null));
                p.add(bd(at, Material.BLACK_CONCRETE, tag, -0.27, 0.75, 0.35, 0.08, 0.22, 0.1, null));
            }
            case "sniper" -> {
                p.add(bd(at, Material.BROWN_CONCRETE, tag, -0.28, 0.95, 0.05, 0.1, 0.14, 0.6, null));
                p.add(bd(at, Material.BLACK_CONCRETE, tag, -0.26, 1.0, 0.65, 0.06, 0.06, 0.9, null));
                p.add(bd(at, Material.BLACK_CONCRETE, tag, -0.265, 1.1, 0.3, 0.07, 0.07, 0.25, null));
            }
            case "para" -> {
                p.add(bd(at, Material.RED_WOOL, tag, -1.3, 2.7, -1.3, 2.6, 0.15, 2.6, null));
                p.add(bd(at, Material.WHITE_WOOL, tag, -0.4, 2.85, -1.3, 0.8, 0.1, 2.6, null));
                p.add(bd(at, Material.RED_WOOL, tag, -0.9, 2.55, -0.9, 1.8, 0.15, 1.8, null));
                for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2)
                    p.add(bd(at, Material.WHITE_CONCRETE, tag, sx * 0.55 - 0.02, 1.9, sz * 0.55 - 0.02, 0.04, 0.7, 0.04, null));
            }
            default -> { return; }
        }
        models.put(e.getUniqueId(), md);
    }

    void modelTick() {
        for (Iterator<Map.Entry<UUID, Mdl>> it = models.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Mdl> en = it.next();
            Mdl md = en.getValue();
            Entity host = Bukkit.getEntity(en.getKey());
            md.age++;
            boolean landed = "para".equals(md.kind) && md.age > 15 && host != null && host.isOnGround();
            if (host == null || host.isDead() || !host.isValid() || landed) {
                for (BlockDisplay part : md.parts) part.remove();
                if (landed && host instanceof LivingEntity le) le.removePotionEffect(PotionEffectType.SLOW_FALLING);
                it.remove();
                continue;
            }
            Location l = host.getLocation();
            float yaw = l.getYaw();
            if (Math.abs(l.getX() - md.lx) < 0.02 && Math.abs(l.getY() - md.ly) < 0.02
                    && Math.abs(l.getZ() - md.lz) < 0.02 && md.lyaw == yaw) continue;
            md.lx = l.getX(); md.ly = l.getY(); md.lz = l.getZ(); md.lyaw = yaw;
            Location tl = l.clone();
            tl.setPitch(0f);
            for (BlockDisplay part : md.parts) if (part.isValid()) part.teleport(tl);
        }
    }

    String modelKind(LivingEntity e, String role) {
        return switch (role) {
            case "tank", "mortar", "bomber", "gunner", "sniper", "para" -> role;
            case "kamikaze" -> e instanceof Vex ? "drone" : null;
            default -> null;
        };
    }

    // ===== Быстрый цикл (каждые 2 тика) =====
    void fastTick() {
        c++;
        List<LivingEntity> ec = new ArrayList<>();
        List<Entity> ac = new ArrayList<>();
        for (Iterator<UUID> it = mobs.iterator(); it.hasNext(); ) {
            UUID id = it.next();
            Entity en = Bukkit.getEntity(id);
            if (en == null) { it.remove(); stuck.remove(id); continue; }
            if (!(en instanceof LivingEntity e) || e.isDead() || !e.isValid()) { it.remove(); stuck.remove(id); continue; }
            String role = role(e);
            String bk = baseOf(e);
            Base b = bk == null ? null : bases.get(bk);
            if (b == null || b.w() == null) { e.remove(); it.remove(); stuck.remove(id); continue; }
            if (!e.getWorld().equals(b.w())) continue;
            Location bl = b.loc();
            if (!"negotiator".equals(role)) {
                ec.add(e);
                if (air(e)) ac.add(e);
                if (!models.containsKey(id) && c % 10 == 0) {
                    String mk = modelKind(e, role);
                    if (mk != null) attachModel(e, mk);
                }
            }

            if ("bomber".equals(role)) { bomberTick(e, b); continue; }

            Stuck s = stuck.computeIfAbsent(id, key -> new Stuck());
            Player tp = nearest(e.getLocation(), 40);
            Location dest = (tp != null && !"negotiator".equals(role)) ? tp.getLocation() : bl;

            if (s.fly > 0) { fly(e, s, dest); continue; }
            if (c % 10 == 0) stuckCheck(e, s, dest, role);
            if (c % 8 == 0) steer(e, role, bl, tp);

            if ("gunner".equals(role) && c % 2 == 0 && tp != null
                    && tp.getLocation().distance(e.getLocation()) < 30 && e.hasLineOfSight(tp)) {
                shootArrow(e, tp, 2.4, cd("specials.gunner-damage"), 0.04);
            }
            if ("sniper".equals(role) && c % 20 == 0) {
                Player sp = nearest(e.getLocation(), 70);
                if (sp != null && e.hasLineOfSight(sp)) shootArrow(e, sp, 4.0, cd("specials.sniper-damage"), 0.0);
            }
            if ("tank".equals(role) && now() % Math.max(2, ci("specials.tank-reload-ticks")) < 2 && tp != null
                    && tp.getLocation().distance(e.getLocation()) < 40 && e.hasLineOfSight(tp)) {
                Vector v = tp.getEyeLocation().toVector().subtract(e.getEyeLocation().toVector()).normalize();
                Fireball f = e.launchProjectile(Fireball.class, v);
                f.setYield(1.8f);
                f.setIsIncendiary(false);
                e.getWorld().playSound(e.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.5f);
            }
            if ("mortar".equals(role) && now() % Math.max(2, ci("specials.mortar-reload-ticks")) < 2) {
                Player mp = nearest(e.getLocation(), 70);
                Location target = mp != null ? mp.getLocation() : bl;
                double dd = target.distance(e.getLocation());
                if (dd >= 15 && dd <= 75) {
                    Location from = e.getEyeLocation().add(0, 1, 0);
                    Entity ent = e.getWorld().spawnEntity(from, EntityType.TNT);
                    if (ent instanceof TNTPrimed t) {
                        Vector h = target.toVector().subtract(from.toVector());
                        h.setY(0);
                        double dist = h.length();
                        if (dist > 0) h.normalize().multiply(Math.min(2.5, dist / 26.0));
                        h.setY(0.65);
                        t.setVelocity(h);
                        t.setFuseTicks(60);
                        e.getWorld().playSound(from, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.5f);
                    }
                }
            }
        }
        tickBees(ec, ac);
        enemyCache = ec;
        airCache = ac;
        allyTick();
        villagerTick();
        modelTick();
        siren();
    }

    void shootArrow(LivingEntity e, Player target, double speed, double damage, double spread) {
        Location eye = e.getEyeLocation();
        Vector v = target.getEyeLocation().toVector().subtract(eye.toVector());
        if (v.lengthSquared() == 0) return;
        v.normalize();
        if (spread > 0) v.add(new Vector(rnd.nextGaussian() * spread, rnd.nextGaussian() * spread, rnd.nextGaussian() * spread));
        v.multiply(speed);
        Arrow a = e.launchProjectile(Arrow.class, v);
        a.setDamage(damage / speed);
        a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        e.getWorld().playSound(eye, Sound.ENTITY_SKELETON_SHOOT, 0.6f, spread > 0 ? 1.6f : 0.8f);
        Bukkit.getScheduler().runTaskLater(this, () -> { if (a.isValid()) a.remove(); }, 60L);
    }

    void steer(LivingEntity e, String role, Location base, Player tp) {
        if (!(e instanceof Mob m)) return;
        double dd = e.getLocation().distance(base);
        if ("negotiator".equals(role)) { if (dd > 6) hop(e, base, 0.25); return; }
        if ("mortar".equals(role) || "sniper".equals(role)) {
            double hold = "mortar".equals(role) ? 42 : 55;
            if (dd > hold) hop(e, base, 0.3); else m.setAware(false);
            return;
        }
        LivingEntity al = nearestIn(allyCache, e.getLocation(), 12);
        if (al != null) m.setTarget(al);
        else if (tp != null) m.setTarget(tp);
        if (isFlyer(e)) {
            if (tp == null && al == null) e.setVelocity(base.toVector().subtract(e.getLocation().toVector()).normalize().multiply(0.3));
            return;
        }
        if (tp == null && al == null) hop(e, base, 0.3);
    }

    /** Spigot не строит путь, поэтому идём к цели прыжками. */
    void hop(LivingEntity e, Location to, double speed) {
        if (!e.isOnGround()) return;
        Vector v = to.toVector().subtract(e.getLocation().toVector());
        v.setY(0);
        if (v.lengthSquared() == 0) return;
        v.normalize().multiply(speed);
        v.setY(0.42);
        e.setVelocity(v);
    }

    void stuckCheck(LivingEntity e, Stuck s, Location dest, String role) {
        if (isFlyer(e)) return;
        if (("mortar".equals(role) || "sniper".equals(role)) && e instanceof Mob m1 && !m1.isAware()) return;
        if (e instanceof Mob m2 && m2.getTarget() != null
                && m2.getTarget().getWorld().equals(e.getWorld())
                && m2.getTarget().getLocation().distance(e.getLocation()) < 18) { s.bad = 0; return; }
        if (e.getLocation().distance(dest) < 6) { s.bad = 0; return; }
        if (s.last != null && s.last.getWorld().equals(e.getWorld()) && s.last.distance(e.getLocation()) < 0.5) s.bad++;
        else s.bad = 0;
        s.last = e.getLocation();
        if (s.bad >= 2) { s.fly = 24; s.bad = 0; }
    }

    /** Застрял: подлетаем вверх, затем вылетаем по направлению к цели. */
    void fly(LivingEntity e, Stuck s, Location dest) {
        int step = 24 - s.fly;
        e.setGravity(false);
        if (step < 10) {
            if (e.getLocation().getBlock().getType().isSolid()) e.teleport(e.getLocation().add(0, 1, 0));
            e.setVelocity(new Vector(0, 0.45, 0));
        } else {
            Vector d = dest.toVector().subtract(e.getLocation().toVector());
            d.setY(0);
            if (d.lengthSquared() > 0) d.normalize().multiply(0.3);
            d.setY(0.05);
            e.setVelocity(d);
        }
        if (--s.fly <= 0) { e.setGravity(true); e.setFallDistance(0f); }
    }

    // ===== Бомбардировщик =====
    void bomberTick(LivingEntity e, Base b) {
        Player p = nearest(e.getLocation(), 90);
        Location tgt = p != null ? p.getLocation() : b.loc();
        Location pos = e.getLocation();
        double dx = tgt.getX() - pos.getX(), dz = tgt.getZ() - pos.getZ();
        double horiz = Math.hypot(dx, dz);
        Vector v;
        if (horiz < 0.01) v = new Vector(0.5, 0, 0);
        else if (horiz > 10) v = new Vector(dx, 0, dz).normalize().multiply(0.55);
        else v = new Vector(-dz, 0, dx).normalize().multiply(0.55);
        double wantY = tgt.getY() + 18;
        v.setY(Math.max(-0.3, Math.min(0.3, (wantY - pos.getY()) * 0.15)));
        e.setGravity(false);
        e.setVelocity(v);
        e.setRotation((float) Math.toDegrees(Math.atan2(-v.getX(), v.getZ())), 0f);
        if (horiz < 28 && now() % Math.max(2, ci("specials.bomb-interval-ticks")) < 2) {
            Entity ent = e.getWorld().spawnEntity(pos.clone().add(0, -1, 0), EntityType.TNT);
            if (ent instanceof TNTPrimed t) {
                t.setFuseTicks(40);
                t.setVelocity(new Vector(v.getX() * 0.3, -0.1, v.getZ() * 0.3));
                e.getWorld().playSound(pos, Sound.ENTITY_TNT_PRIMED, 1f, 1f);
            }
        }
    }

    // ===== Пчёлы и дроны-смертники =====
    void tickBees(List<LivingEntity> ec, List<Entity> ac) {
        for (Iterator<Map.Entry<UUID, Integer>> it = bees.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Integer> en = it.next();
            Entity ent = Bukkit.getEntity(en.getKey());
            if (ent == null) { it.remove(); continue; }
            if (!(ent instanceof Mob bee) || bee.isDead()) { it.remove(); continue; }
            String bk = baseOf(bee);
            Base b = bk == null ? null : bases.get(bk);
            if (b == null || b.w() == null) { boom(bee); it.remove(); continue; }
            ec.add(bee);
            ac.add(bee);
            if (!models.containsKey(bee.getUniqueId()) && bee instanceof Vex && c % 10 == 0) attachModel(bee, "drone");
            int age = en.getValue() + 1;
            en.setValue(age);
            Location l = bee.getLocation();
            Player p = nearest(l, 90);
            Location target = p != null ? p.getEyeLocation() : b.loc().add(0, 1, 0);
            Vector vel = target.toVector().subtract(l.toVector());
            if (vel.lengthSquared() < 0.01) { boom(bee); it.remove(); continue; }
            vel.normalize().multiply(bee instanceof Vex ? 0.5 : 0.42);
            bee.setVelocity(vel);
            if (bee instanceof Vex) bee.setRotation((float) Math.toDegrees(Math.atan2(-vel.getX(), vel.getZ())), 0f);
            if (age > 10) {
                Location ahead = l.clone().add(vel.clone().normalize().multiply(0.8));
                boolean hit = ahead.getBlock().getType().isSolid() || l.getBlock().getType().isSolid()
                        || (p != null && p.getLocation().add(0, 1, 0).distance(l) < 1.6);
                if (hit || age > 900) { boom(bee); it.remove(); }
            }
        }
    }

    void boom(Entity bee) {
        Location l = bee.getLocation();
        float power = (float) (bee instanceof Vex ? cd("specials.drone-explosion") : cd("specials.bee-explosion"));
        for (Entity p : new ArrayList<>(bee.getPassengers())) p.remove();
        bee.remove();
        l.getWorld().createExplosion(l, power, false, true);
    }

    // ===== Зенитки =====
    int upgradeCost(int level) { return ci("turret.upgrade-base") + ci("turret.upgrade-per-level") * level; }

    double turretRange(Turret t) { return cd("turret.range-base") + cd("turret.range-per-level") * (t.level - 1); }

    double turretRate(Turret t) { return Math.min(20.0, cd("turret.shots-per-level") * t.level); }

    void turretTick() {
        for (Turret t : new ArrayList<>(turrets.values())) {
            World w = t.w();
            if (w == null || !w.isChunkLoaded(t.x >> 4, t.z >> 4)) continue;
            if (w.getBlockAt(t.x, t.y, t.z).getType() != TURRET_MAT) { removeTurret(t); continue; }
            if (t.modelLevel != t.level || t.parts.isEmpty() || !t.parts.get(0).isValid()) buildTurretModel(t);
            double range = turretRange(t);
            Location top = t.top();
            if (t.target == null || !t.target.isValid() || t.target.isDead() || !t.target.getWorld().equals(w)
                    || t.target.getLocation().distanceSquared(top) > range * range || c % 3 == 0) {
                Entity best = null; double bd2 = range * range;
                for (Entity en : airCache) {
                    if (en.isDead() || !en.isValid() || !en.getWorld().equals(w)) continue;
                    double dd = en.getLocation().distanceSquared(top);
                    if (dd < bd2) { bd2 = dd; best = en; }
                }
                t.target = best;
            }
            if (t.target == null) { t.acc = Math.min(t.acc, 1.0); continue; }
            t.acc += turretRate(t) / 20.0;
            int n = 0;
            while (t.acc >= 1.0 && n < 4) { fireTurret(t, top, t.target); t.acc -= 1.0; n++; }
            if (t.acc > 2) t.acc = 2;
            if (n > 0) w.playSound(top, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.8f + t.level * 0.03f);
        }
    }

    void fireTurret(Turret t, Location top, Entity tgt) {
        World w = top.getWorld();
        Location mz = top.clone().add((rnd.nextDouble() - 0.5) * 0.4, 0, (rnd.nextDouble() - 0.5) * 0.4);
        double dist = tgt.getLocation().distance(mz);
        Location tl = tgt.getLocation().add(0, tgt.getHeight() / 2, 0).add(tgt.getVelocity().multiply(dist / 1.5));
        Vector dir = tl.toVector().subtract(mz.toVector());
        if (dir.lengthSquared() == 0) return;
        dir.normalize();
        Fireball fb = w.spawn(mz, Fireball.class);
        fb.setDirection(dir);
        fb.setVelocity(dir.clone().multiply(cd("turret.fireball-speed")));
        fb.setYield((float) (cd("turret.yield-base") + cd("turret.yield-per-level") * t.level));
        fb.setIsIncendiary(false);
        fb.getPersistentDataContainer().set(AA, PersistentDataType.BYTE, (byte) 1);
        Bukkit.getScheduler().runTaskLater(this, () -> { if (fb.isValid()) fb.remove(); }, 50L);
        w.spawnParticle(Particle.FLAME, mz, 3, 0.1, 0.1, 0.1, 0.02);
    }

    void removeTurret(Turret t) {
        turrets.remove(t.key());
        for (BlockDisplay part : t.parts) part.remove();
        t.parts.clear();
        save();
    }

    int barrels(int level) { return Math.min(10, 1 + (level - 1) / 2 + (level >= 20 ? 1 : 0)); }

    void tpart(Turret t, Location o, Material m, float tx, float ty, float tz,
               float sx, float sy, float sz, Quaternionf rot, boolean glow) {
        BlockDisplay d = bd(o, m, "t:" + t.key(), tx, ty, tz, sx, sy, sz, rot);
        d.setTeleportDuration(0);
        if (glow) d.setGlowing(true);
        t.parts.add(d);
    }

    /** У каждого из 20 уровней свой облик: материалы, число стволов, размеры, пилоны, радар, крылья, корона. */
    void buildTurretModel(Turret t) {
        World w = t.w();
        if (w == null) return;
        for (BlockDisplay part : t.parts) part.remove();
        t.parts.clear();
        Location o = new Location(w, t.x + 0.5, t.y + 1, t.z + 0.5);
        for (Entity en : w.getNearbyEntities(o, 3, 6, 3)) {
            if (en instanceof BlockDisplay && ("t:" + t.key()).equals(en.getPersistentDataContainer().get(MODEL, PersistentDataType.STRING))) en.remove();
        }
        int L = t.level, i = L - 1;
        tpart(t, o, AA_BODY[i], -0.5f, 0f, -0.5f, 1f, 0.2f, 1f, null, false);
        float ch = 0.3f + 0.02f * L, cw = 0.45f + 0.01f * L;
        tpart(t, o, AA_BODY[i], -cw / 2, 0.2f, -cw / 2, cw, ch, cw, null, false);
        float hy = 0.2f + ch, hw = 0.6f + 0.015f * L;
        tpart(t, o, AA_HEAD[i], -hw / 2, hy, -hw / 2, hw, 0.3f, hw, null, L >= 15);
        int n = barrels(L);
        float r = Math.min(0.3f, 0.1f + 0.03f * n), len = 0.6f + 0.05f * L, bw = 0.1f;
        for (int k = 0; k < n; k++) {
            double a = 2 * Math.PI * k / n;
            float cx = (float) Math.cos(a) * r, cz = (float) Math.sin(a) * r;
            Quaternionf q = new Quaternionf().rotateAxis((float) Math.toRadians(10 + L / 2.0),
                    (float) Math.sin(a), 0f, (float) -Math.cos(a));
            tpart(t, o, AA_BARREL[i], cx - bw / 2, hy + 0.3f, cz - bw / 2, bw, len, bw, q, false);
        }
        if (L >= 5) {
            for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2)
                tpart(t, o, AA_BODY[i], sx * 0.42f - 0.06f, 0.2f, sz * 0.42f - 0.06f, 0.12f, 0.35f, 0.12f, null, false);
        }
        if (L >= 10) {
            tpart(t, o, Material.LIGHTNING_ROD, 0.3f, 0.2f, -0.45f, 0.1f, 0.9f, 0.1f, null, false);
            tpart(t, o, Material.TARGET, 0.1f, 1.05f, -0.65f, 0.5f, 0.1f, 0.5f, null, L >= 15);
        }
        if (L >= 15) {
            tpart(t, o, AA_BODY[i], -1.0f, 0.2f, -0.15f, 0.5f, 0.12f, 0.3f, null, false);
            tpart(t, o, AA_BODY[i], 0.5f, 0.2f, -0.15f, 0.5f, 0.12f, 0.3f, null, false);
        }
        if (L >= 20) tpart(t, o, Material.BEACON, -0.2f, hy + 0.31f, -0.2f, 0.4f, 0.4f, 0.4f, null, true);
        t.modelLevel = L;
    }

    // ===== Жители-ополченцы: бой, ремонт, эмоции =====
    void recruit(Base b) {
        if (!cb("villagers.help")) return;
        World w = b.w();
        int max = ci("villagers.max");
        int have = 0;
        for (UUID id : defenders) {
            Entity en = Bukkit.getEntity(id);
            if (en != null && en.getWorld().equals(w) && en.getLocation().distance(b.loc()) < 90) have++;
        }
        for (Entity en : w.getNearbyEntities(b.loc(), 80, 50, 80)) {
            if (have >= max) break;
            if (en instanceof Villager v && v.isAdult() && !isMilitia(v) && rnd.nextDouble() < cd("villagers.chance")) {
                enlist(v);
                have++;
            }
        }
    }

    void emote(Villager v, String key, double chance) {
        if (!cb("villagers.emotions") || rnd.nextDouble() > chance) return;
        long t = now();
        Long last = lastEmote.get(v.getUniqueId());
        if (last != null && t - last < ci("villagers.emote-cooldown-ticks")) return;
        if (bubbles >= 40) return;
        String[] arr = EMO.get(key);
        if (arr == null) return;
        lastEmote.put(v.getUniqueId(), t);
        bubble(v, arr[rnd.nextInt(arr.length)]);
    }

    void emoteAround(Location l, double r, String key, double chance) {
        for (UUID id : new ArrayList<>(defenders)) {
            Entity en = Bukkit.getEntity(id);
            if (en instanceof Villager v && v.getWorld().equals(l.getWorld()) && v.getLocation().distanceSquared(l) <= r * r)
                emote(v, key, chance);
        }
    }

    void bubble(Villager v, String text) {
        Location at = v.getEyeLocation().add(0, 0.9, 0);
        TextDisplay td = v.getWorld().spawn(at, TextDisplay.class);
        td.setText(ChatColor.WHITE + text);
        td.setBillboard(Display.Billboard.CENTER);
        td.setBackgroundColor(Color.fromARGB(170, 0, 0, 0));
        td.setSeeThrough(false);
        td.setShadowed(false);
        td.setLineWidth(160);
        td.setViewRange(1.2f);
        td.setTeleportDuration(2);
        td.setPersistent(false);
        bubbles++;
        new BukkitRunnable() {
            int life = 0;
            @Override public void run() {
                if (life++ > 30 || !v.isValid() || !td.isValid()) { td.remove(); bubbles = Math.max(0, bubbles - 1); cancel(); return; }
                td.teleport(v.getEyeLocation().add(0, 0.9, 0));
            }
        }.runTaskTimer(this, 2L, 2L);
    }

    void villagerTick() {
        if (defenders.isEmpty()) return;
        long t = now();
        for (Iterator<UUID> it = defenders.iterator(); it.hasNext(); ) {
            UUID id = it.next();
            Entity en = Bukkit.getEntity(id);
            if (en == null) continue;
            if (!(en instanceof Villager v) || v.isDead() || !isMilitia(v)) { it.remove(); continue; }
            Base wb = baseNear(v.getLocation(), 110, true);
            if (wb != null && cb("villagers.help")) fightStep(v, wb, t);
            Base rb = baseNear(v.getLocation(), 110, false);
            if (rb != null && !rb.repairs.isEmpty() && cb("villagers.repair") && (wb == null || nearestEnemyDist(v) > 14)) repairStep(v, rb, t);
        }
    }

    double nearestEnemyDist(Villager v) {
        LivingEntity e = nearestIn(enemyCache, v.getLocation(), 45);
        return e == null ? 999 : e.getLocation().distance(v.getLocation());
    }

    void fightStep(Villager v, Base b, long t) {
        int w = vi(v, V_WEAPON), aim = vi(v, V_AIM), rate = vi(v, V_RATE);
        LivingEntity target = nearestIn(enemyCache, v.getLocation(), w == 4 ? 34 : 26);
        if (target == null) return;
        if (target.getLocation().distance(b.loc()) > 60) return;
        double dist = target.getLocation().distance(v.getLocation());
        long ns = nextShot.getOrDefault(v.getUniqueId(), 0L);
        if (t < ns) return;
        if (dist > (w == 4 ? 30 : 22) || !v.hasLineOfSight(target)) {
            if (dist > 12) hop(v, target.getLocation(), 0.18);
            return;
        }
        int interval = (int) Math.max(2, WINT[w] * (1.0 - 0.12 * rate));
        nextShot.put(v.getUniqueId(), t + interval);
        Location eye = v.getEyeLocation();
        Vector dir = target.getEyeLocation().toVector().subtract(eye.toVector());
        if (dir.lengthSquared() == 0) return;
        double spread = Math.max(0.0, WSPREAD[w] - 0.008 * aim);
        if (w == 0) {
            dir.setY(dir.getY() + dist * 0.12);
            dir.normalize().multiply(1.2);
            Snowball sb = v.launchProjectile(Snowball.class, dir);
            int dmg = ci("villagers.throw-damage");
            if (rnd.nextInt(10) < 4) { sb.setItem(new ItemStack(Material.STICK)); dmg = ci("villagers.stick-damage"); }
            sb.getPersistentDataContainer().set(THROW, PersistentDataType.INTEGER, dmg + aim / 2);
            v.getWorld().playSound(eye, Sound.ENTITY_SNOWBALL_THROW, 1f, 1f);
        } else if (w == 4) {
            dir.setY(dir.getY() + dist * 0.18);
            dir.normalize().multiply(1.1);
            Snowball sb = v.launchProjectile(Snowball.class, dir);
            sb.setItem(make("grenade", 1));
            sb.getPersistentDataContainer().set(GREN, PersistentDataType.STRING, "frag");
            v.getWorld().playSound(eye, Sound.ENTITY_SNOWBALL_THROW, 1f, 0.7f);
        } else {
            dir.normalize();
            dir.add(new Vector(rnd.nextGaussian() * spread, rnd.nextGaussian() * spread, rnd.nextGaussian() * spread)).normalize();
            double speed = 3.5;
            dir.multiply(speed);
            Arrow a = v.launchProjectile(Arrow.class, dir);
            a.setDamage((WDMG[w] + aim * 0.5) / speed);
            a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            a.setGravity(false);
            a.getPersistentDataContainer().set(BULLET, PersistentDataType.STRING, "v");
            Bukkit.getScheduler().runTaskLater(this, () -> { if (a.isValid()) a.remove(); }, 40L);
            v.getWorld().playSound(eye, Sound.ENTITY_SKELETON_SHOOT, 0.5f, 1.8f);
        }
        emote(v, "fire", 0.08);
    }

    void repairStep(Villager v, Base b, long t) {
        long nr = nextRepair.getOrDefault(v.getUniqueId(), 0L);
        if (t < nr) return;
        int interval = Math.max(2, ci("villagers.repair-interval-ticks") - 2 * vi(v, V_REPAIR));
        Location vl = v.getLocation();
        int bestI = -1; double bs = Double.MAX_VALUE;
        List<Rep> list = b.repairs;
        for (int i = 0; i < list.size(); i++) {
            Rep r = list.get(i);
            if (!r.loc.getWorld().equals(vl.getWorld())) continue;
            double dd = r.loc.distance(vl) + 3.0 * (r.loc.getY() - b.y);
            if (dd < bs) { bs = dd; bestI = i; }
        }
        if (bestI < 0) return;
        Rep r = list.get(bestI);
        if (r.loc.distance(vl) > 5.0) {
            if (r.loc.distance(vl) < 60) hop(v, r.loc, 0.2);
            nextRepair.put(v.getUniqueId(), t + 4);
            return;
        }
        nextRepair.put(v.getUniqueId(), t + interval);
        list.set(bestI, list.get(list.size() - 1));
        list.remove(list.size() - 1);
        Block bl = r.loc.getBlock();
        Material cur = bl.getType();
        if (cur.isAir() || cur == Material.WATER || cur == Material.LAVA || cur == Material.FIRE || cur == Material.SHORT_GRASS) {
            bl.setBlockData(r.data, false);
            bl.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, bl.getLocation().add(0.5, 0.5, 0.5), 4, 0.3, 0.3, 0.3, 0.05);
            bl.getWorld().playSound(bl.getLocation(), Sound.BLOCK_STONE_PLACE, 0.7f, 1f);
            emote(v, "repair", 0.12);
        }
    }

    void recordRepairs(Location origin, List<Block> blocks) {
        if (!cb("villagers.repair") || blocks.isEmpty()) return;
        double r = cd("villagers.repair-radius");
        for (Base b : bases.values()) {
            if (b.state != State.WAR || b.w() == null || !b.w().equals(origin.getWorld())) continue;
            if (b.loc().distanceSquared(origin) > r * r) continue;
            for (Block bl : blocks) {
                if (b.repairs.size() >= ci("villagers.repair-max-queue")) break;
                Material m = bl.getType();
                if (m.isAir() || isBase(bl) || isTurretBlock(bl) || m == Material.TNT) continue;
                if (bl.getState() instanceof TileState) continue;
                b.repairs.add(new Rep(bl.getLocation(), bl.getBlockData()));
            }
        }
        emoteAround(origin, 14, "wallbreak", 0.35);
    }

    // ===== Союзники (рация) =====
    void callAllies(Player p) {
        long nowMs = System.currentTimeMillis();
        Long until = radioCd.get(p.getUniqueId());
        if (until != null && nowMs < until) {
            p.sendMessage(ChatColor.RED + "Рация перезаряжается: ещё " + ((until - nowMs) / 1000 + 1) + " с");
            return;
        }
        radioCd.put(p.getUniqueId(), nowMs + ci("radio.cooldown-seconds") * 1000L);
        Base nb = baseNear(p.getLocation(), 300, false);
        int wave = nb == null ? 1 : Math.max(1, nb.wave);
        int n = ci("radio.count") + (wave / 3) * ci("radio.per-3-waves");
        World w = p.getWorld();
        for (int i = 0; i < n; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2, rr = 3 + rnd.nextDouble() * 4;
            Location l = p.getLocation().add(Math.cos(ang) * rr, 0.2, Math.sin(ang) * rr);
            EntityType t = ALLY_TYPES[Math.min(ALLY_TYPES.length - 1, rnd.nextInt(2 + Math.min(wave, ALLY_TYPES.length - 2)))];
            Entity ent = w.spawnEntity(l, t);
            if (!(ent instanceof Mob m)) { ent.remove(); continue; }
            m.getPersistentDataContainer().set(ALLY, PersistentDataType.STRING, p.getUniqueId().toString());
            m.setRemoveWhenFarAway(false);
            m.setCanPickupItems(false);
            m.setCustomName(ChatColor.BLUE + "Союзник");
            m.setCustomNameVisible(false);
            m.setGlowing(true);
            AttributeInstance fr = m.getAttribute(Attribute.FOLLOW_RANGE);
            if (fr != null) fr.setBaseValue(60);
            mulAttr(m, Attribute.MAX_HEALTH, cd("radio.health-multiplier"));
            AttributeInstance mh = m.getAttribute(Attribute.MAX_HEALTH);
            if (mh != null) m.setHealth(mh.getBaseValue());
            if (m instanceof Zombie z) z.setBaby(false);
            var eq = m.getEquipment();
            if (eq != null && (m instanceof Zombie || m instanceof AbstractSkeleton)) {
                eq.setHelmet(dye(new ItemStack(Material.LEATHER_HELMET), Color.BLUE));
                eq.setChestplate(dye(new ItemStack(Material.LEATHER_CHESTPLATE), Color.BLUE));
                eq.setLeggings(dye(new ItemStack(Material.LEATHER_LEGGINGS), Color.BLUE));
                eq.setBoots(dye(new ItemStack(Material.LEATHER_BOOTS), Color.BLUE));
                eq.setHelmetDropChance(0f); eq.setChestplateDropChance(0f); eq.setLeggingsDropChance(0f); eq.setBootsDropChance(0f);
            }
            if (allyTeam != null) allyTeam.addEntry(m.getUniqueId().toString());
            allies.put(m.getUniqueId(), now() + ci("radio.lifetime-seconds") * 20L);
            w.spawnParticle(Particle.CLOUD, l.clone().add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.02);
        }
        w.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1f, 1f);
        p.sendMessage(ChatColor.BLUE + "Подкрепление прибыло: " + n + " союзников!");
        emoteAround(p.getLocation(), 40, "ally", 0.6);
    }

    void allyTick() {
        List<LivingEntity> list = new ArrayList<>();
        long t = now();
        for (Iterator<Map.Entry<UUID, Long>> it = allies.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Long> en = it.next();
            Entity e = Bukkit.getEntity(en.getKey());
            if (!(e instanceof Mob m) || m.isDead() || !m.isValid()) { it.remove(); continue; }
            if (t > en.getValue()) {
                m.getWorld().spawnParticle(Particle.POOF, m.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);
                if (allyTeam != null) allyTeam.removeEntry(m.getUniqueId().toString());
                m.remove(); it.remove(); continue;
            }
            list.add(m);
            if (c % 4 != 0) continue;
            LivingEntity foe = nearestIn(enemyCache, m.getLocation(), 40);
            if (foe != null) { m.setTarget(foe); continue; }
            m.setTarget(null);
            String ow = m.getPersistentDataContainer().get(ALLY, PersistentDataType.STRING);
            Player owner = ow == null ? null : Bukkit.getPlayer(UUID.fromString(ow));
            if (owner != null && owner.getWorld().equals(m.getWorld())) {
                double dd = owner.getLocation().distance(m.getLocation());
                if (dd > 45) m.teleport(owner.getLocation());
                else if (dd > 7) hop(m, owner.getLocation(), 0.28);
            }
        }
        allyCache = list;
    }

    // ===== Ядерная бомба =====
    void startNuke(Player p) {
        if (!cb("nuke.enabled")) { p.sendMessage(ChatColor.RED + "Ядерная бомба отключена."); return; }
        if (nukeBusy) { p.sendMessage(ChatColor.RED + "Уже идёт ядерный удар!"); return; }
        Base b = baseNear(p.getLocation(), 400, false);
        if (b == null) { p.sendMessage(ChatColor.RED + "Рядом нет маяка-ядра, цель не найдена."); return; }
        ItemStack hand = p.getInventory().getItemInMainHand();
        hand.setAmount(hand.getAmount() - 1);
        nukeBusy = true;
        final Location center = b.loc();
        final int delay = Math.max(1, ci("nuke.delay-seconds"));
        final double radius = cd("nuke.radius");
        final World w = center.getWorld();
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "☢ ЯДЕРНЫЙ УДАР по базе " + b.x + " " + b.y + " " + b.z
                + "! Всем покинуть зону в радиусе " + (int) radius + " блоков!");
        emoteAround(center, 90, "nuke", 1.0);
        new BukkitRunnable() {
            int left = delay;
            @Override public void run() {
                for (Player pl : w.getPlayers()) {
                    if (pl.getLocation().distanceSquared(center) < (radius * 3) * (radius * 3)) {
                        pl.sendTitle(ChatColor.DARK_RED + "☢ " + left, ChatColor.RED + "Взрыв через " + left + " с", 0, 25, 5);
                        pl.playSound(pl.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 2f, 1.9f);
                    }
                }
                if (left-- <= 0) { cancel(); detonate(b, center, radius); }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    void detonate(Base b, Location center, double radius) {
        World w = center.getWorld();
        removeMobs(b.key(), null);
        for (Entity en : w.getNearbyEntities(center, radius * 1.5, radius * 1.5, radius * 1.5)) {
            if (en instanceof Player pl) {
                if (pl.getGameMode() == GameMode.CREATIVE || pl.getGameMode() == GameMode.SPECTATOR) continue;
                double dd = pl.getLocation().distance(center);
                if (dd <= radius && cb("nuke.kill-players")) pl.setHealth(0);
                else if (dd <= radius * 1.5) pl.damage(Math.max(2, 20 * (1 - dd / (radius * 1.5))));
            } else if (en instanceof LivingEntity le && en.getLocation().distance(center) <= radius * 1.2) {
                if (isEnemy(en) || isAlly(en) || en instanceof Villager) le.setHealth(0);
            } else if (en instanceof Item && en.getLocation().distance(center) <= radius) en.remove();
        }
        for (Turret t : new ArrayList<>(turrets.values()))
            if (t.w() != null && t.w().equals(w) && t.top().distance(center) <= radius) removeTurret(t);
        dropBase(b);
        save();
        for (int i = 0; i < 6; i++) {
            Location l = center.clone().add((rnd.nextDouble() - 0.5) * radius * 0.6, rnd.nextDouble() * 6, (rnd.nextDouble() - 0.5) * radius * 0.6);
            w.spawnParticle(Particle.EXPLOSION_EMITTER, l, 3, 2, 2, 2, 0);
            w.strikeLightningEffect(l);
        }
        for (Player pl : w.getPlayers()) pl.playSound(pl.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 4f, 0.5f);
        final int R = (int) Math.ceil(radius);
        final double depthFactor = cd("nuke.depth-factor");
        final List<int[]> cols = new ArrayList<>();
        for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) if (dx * dx + dz * dz <= radius * radius) cols.add(new int[]{dx, dz});
        cols.sort(Comparator.comparingInt(a -> a[0] * a[0] + a[1] * a[1]));
        final int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();
        final int topH = Math.max(6, (int) (radius * 0.35));
        new BukkitRunnable() {
            int idx = 0;
            @Override public void run() {
                int done = 0;
                while (idx < cols.size() && done < 350) {
                    int[] col = cols.get(idx++);
                    double dd = Math.sqrt(col[0] * col[0] + col[1] * col[1]);
                    int depth = (int) Math.round(radius * depthFactor * Math.sqrt(Math.max(0, 1 - (dd / radius) * (dd / radius))));
                    for (int y = cy + topH; y >= cy - depth; y--) {
                        Block bl = w.getBlockAt(cx + col[0], y, cz + col[1]);
                        Material m = bl.getType();
                        if (m.isAir() || m == Material.BEDROCK) continue;
                        bl.setType(Material.AIR, false);
                    }
                    done++;
                }
                if (idx >= cols.size()) { nukeBusy = false; cancel(); }
            }
        }.runTaskTimer(this, 1L, 1L);
    }

    // ===== Оружие игрока =====
    void shoot(Player p, String id) {
        long t = now();
        int interval = switch (id) {
            case "mg" -> ci("weapons.mg-interval-ticks");
            case "sniper" -> ci("weapons.sniper-interval-ticks");
            default -> ci("weapons.rifle-interval-ticks");
        };
        Long last = playerShot.get(p.getUniqueId());
        if (last != null && t - last < interval) return;
        PlayerInventory inv = p.getInventory();
        int slot = -1;
        for (int i = 0; i < 36; i++) if ("ammo".equals(itemId(inv.getItem(i)))) { slot = i; break; }
        if (slot < 0) {
            if (last == null || t - last > 20) p.sendMessage(ChatColor.RED + "Нет патронов! Купи в военном магазине.");
            playerShot.put(p.getUniqueId(), t);
            p.playSound(p.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 1f, 1.5f);
            return;
        }
        playerShot.put(p.getUniqueId(), t);
        ItemStack am = inv.getItem(slot);
        if (am.getAmount() <= 1) inv.setItem(slot, null); else { am.setAmount(am.getAmount() - 1); inv.setItem(slot, am); }
        double dmg = switch (id) {
            case "mg" -> cd("weapons.mg-damage");
            case "sniper" -> cd("weapons.sniper-damage");
            default -> cd("weapons.rifle-damage");
        };
        double speed = id.equals("sniper") ? cd("weapons.bullet-speed") * 1.6 : cd("weapons.bullet-speed");
        double spread = switch (id) { case "mg" -> 0.05; case "sniper" -> 0.0; default -> 0.025; };
        Vector dir = p.getEyeLocation().getDirection().normalize();
        if (spread > 0) dir.add(new Vector(rnd.nextGaussian() * spread, rnd.nextGaussian() * spread, rnd.nextGaussian() * spread)).normalize();
        dir.multiply(speed);
        Arrow a = p.launchProjectile(Arrow.class, dir);
        a.setDamage(dmg / speed);
        a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        a.setGravity(false);
        a.getPersistentDataContainer().set(BULLET, PersistentDataType.STRING, "p");
        Bukkit.getScheduler().runTaskLater(this, () -> { if (a.isValid()) a.remove(); }, 40L);
        p.getWorld().playSound(p.getLocation(), id.equals("sniper") ? Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST : Sound.ENTITY_SKELETON_SHOOT,
                0.8f, id.equals("sniper") ? 0.8f : 1.9f);
    }

    void explodeGrenade(Location l, String type, Entity src) {
        World w = l.getWorld();
        if (type.equals("fire")) {
            w.createExplosion(l, (float) cd("weapons.grenade-fire-power"), true, false, src);
            w.spawnParticle(Particle.FLAME, l, 40, 1.2, 0.5, 1.2, 0.05);
        } else {
            w.createExplosion(l, (float) cd("weapons.grenade-power"), false, false, src);
        }
    }

    // ===== События =====
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        ItemStack it = e.getItemInHand();
        if (isCore(it)) {
            if (e.getBlock().getWorld().getEnvironment() != World.Environment.NORMAL) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(ChatColor.RED + "Ядро можно ставить только в обычном мире.");
                return;
            }
            Block bl = e.getBlockPlaced();
            Base b = new Base(bl.getWorld().getName(), bl.getX(), bl.getY(), bl.getZ());
            b.countdown = ci("timing.wait-seconds");
            bases.put(b.key(), b);
            save();
            e.getPlayer().sendMessage(ChatColor.DARK_PURPLE + "Ядро установлено. ПКМ по маяку — военный магазин. Скоро к вам придут гости...");
            return;
        }
        if ("turret".equals(itemId(it))) {
            Block bl = e.getBlockPlaced();
            Turret t = new Turret(bl.getWorld().getName(), bl.getX(), bl.getY(), bl.getZ());
            Integer lv = it.getItemMeta().getPersistentDataContainer().get(TLEVEL, PersistentDataType.INTEGER);
            t.level = lv == null ? 1 : Math.max(1, Math.min(20, lv));
            turrets.put(t.key(), t);
            save();
            e.getPlayer().sendMessage(ChatColor.AQUA + "Зенитка установлена (ур. " + t.level + "). ПКМ землёй — улучшить.");
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Block blk = e.getBlock();
        Base b = bases.get(keyOf(blk));
        if (b != null) {
            dropBase(b);
            save();
            e.setDropItems(false);
            blk.getWorld().dropItemNaturally(blk.getLocation().add(0.5, 0.5, 0.5), core(1));
            e.getPlayer().sendMessage(ChatColor.GRAY + "Ядро убрано. Мобы отступили.");
            return;
        }
        Turret t = turrets.get(keyOf(blk));
        if (t != null) {
            e.setDropItems(false);
            blk.getWorld().dropItemNaturally(blk.getLocation().add(0.5, 0.5, 0.5), turretItem(1, t.level));
            removeTurret(t);
            return;
        }
        if (isDirt(blk.getType()) && e.getPlayer().getGameMode() == GameMode.SURVIVAL) {
            String id = itemId(e.getPlayer().getInventory().getItemInMainHand());
            if (id != null && id.startsWith("shovel_")) {
                int bonus = ci("shovel.bonus-" + id.substring(7));
                if (bonus > 0) blk.getWorld().dropItemNaturally(blk.getLocation().add(0.5, 0.5, 0.5), new ItemStack(Material.DIRT, bonus));
            }
        }
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Action act = e.getAction();
        if (act != Action.RIGHT_CLICK_AIR && act != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        String id = itemId(hand);
        if (id != null) {
            switch (id) {
                case "rifle", "mg", "sniper" -> { e.setCancelled(true); shoot(p, id); return; }
                case "radio" -> { e.setCancelled(true); callAllies(p); return; }
                case "nuke" -> { e.setCancelled(true); startNuke(p); return; }
                case "grenade", "grenade_fire" -> { return; }
                default -> { }
            }
        }
        if (act != Action.RIGHT_CLICK_BLOCK) return;
        Block bl = e.getClickedBlock();
        if (bl == null) return;
        Base b = bases.get(keyOf(bl));
        if (b != null) {
            if (isCore(hand)) return;
            e.setCancelled(true);
            openShop(p);
            return;
        }
        Turret t = turrets.get(keyOf(bl));
        if (t == null) return;
        if ("turret".equals(id)) return;
        e.setCancelled(true);
        if (isDirt(hand.getType()) && !hand.hasItemMeta()) {
            if (t.level >= 20) { p.sendMessage(ChatColor.GOLD + "Зенитка уже максимального уровня: " + AA_NAMES[19]); return; }
            int cost = upgradeCost(t.level);
            int take = Math.min(cost - t.dirt, hand.getAmount());
            hand.setAmount(hand.getAmount() - take);
            t.dirt += take;
            if (t.dirt >= cost) {
                t.dirt -= cost;
                t.level++;
                if (t.level >= 20) t.dirt = 0;
                buildTurretModel(t);
                Location fx = t.top();
                t.w().spawnParticle(Particle.HAPPY_VILLAGER, fx, 40, 0.6, 0.8, 0.6, 0.1);
                t.w().playSound(fx, Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
                say(p.getName() + " улучшил зенитку до уровня " + t.level + ": " + AA_NAMES[t.level - 1] + "!");
                emoteAround(fx, 40, "turret", 0.4);
            } else {
                p.sendMessage(ChatColor.GREEN + "Земля: " + t.dirt + "/" + cost + " до уровня " + (t.level + 1));
            }
            save();
            return;
        }
        if (hand.getType() == Material.AIR) {
            p.sendMessage(ChatColor.AQUA + "Зенитка: ур. " + t.level + "/20 «" + AA_NAMES[t.level - 1] + "». Дальность "
                    + (int) turretRange(t) + " бл., скорострельность " + String.format("%.0f", turretRate(t)) + " выстр./с. "
                    + (t.level < 20 ? "Земля: " + t.dirt + "/" + upgradeCost(t.level) + "." : "Максимальный уровень!"));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Menu m)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getInventory()) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;
        int slot = e.getRawSlot();
        if (m.type.equals("shop")) {
            for (String[] en : SHOP) if (Integer.parseInt(en[1]) == slot) { buy(p, en[0], Integer.parseInt(en[2]), e.getInventory()); return; }
        } else if (m.type.equals("militia")) {
            militiaClick(p, m, slot, e.getInventory());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Menu) e.setCancelled(true);
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e) {
        for (Block bl : e.getBlocks()) if (isBase(bl) || isTurretBlock(bl)) { e.setCancelled(true); return; }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent e) {
        for (Block bl : e.getBlocks()) if (isBase(bl) || isTurretBlock(bl)) { e.setCancelled(true); return; }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e) {
        if (e.getEntity().getPersistentDataContainer().has(AA, PersistentDataType.BYTE)) {
            e.blockList().clear();
            return;
        }
        e.blockList().removeIf(bl -> isBase(bl) || isTurretBlock(bl));
        recordRepairs(e.getLocation(), e.blockList());
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(bl -> isBase(bl) || isTurretBlock(bl));
        recordRepairs(e.getBlock().getLocation(), e.blockList());
    }

    /** Огонь зенитки и союзные пули бьют только врагов. */
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        var pdc = e.getDamager().getPersistentDataContainer();
        Entity victim = e.getEntity();
        if (pdc.has(AA, PersistentDataType.BYTE)) {
            if (!isEnemy(victim)) e.setCancelled(true);
            return;
        }
        String bullet = pdc.get(BULLET, PersistentDataType.STRING);
        if (bullet != null) {
            if (bullet.equals("v") && !isEnemy(victim)) { e.setCancelled(true); return; }
            if (bullet.equals("p") && (victim instanceof Villager || isAlly(victim) || victim instanceof Player)) { e.setCancelled(true); return; }
            if (victim instanceof LivingEntity le) Bukkit.getScheduler().runTask(this, () -> le.setNoDamageTicks(0));
        }
    }

    @EventHandler
    public void onHurt(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Villager v) || !isMilitia(v)) return;
        int a = vi(v, V_ARMOR);
        if (a > 0) e.setDamage(e.getDamage() * (1 - Math.min(0.9, a * cd("villagers.armor-reduction-per-tier"))));
        emote(v, "hurt", 0.3);
    }

    @EventHandler
    public void onLaunch(ProjectileLaunchEvent e) {
        if (!(e.getEntity() instanceof Snowball sb) || !(sb.getShooter() instanceof Player)) return;
        String id = itemId(sb.getItem());
        if (id == null) return;
        if (id.equals("grenade")) sb.getPersistentDataContainer().set(GREN, PersistentDataType.STRING, "frag");
        else if (id.equals("grenade_fire")) sb.getPersistentDataContainer().set(GREN, PersistentDataType.STRING, "fire");
    }

    @EventHandler
    public void onHit(ProjectileHitEvent e) {
        var pdc = e.getEntity().getPersistentDataContainer();
        String gren = pdc.get(GREN, PersistentDataType.STRING);
        if (gren != null) {
            Location l = e.getEntity().getLocation();
            Object sh = e.getEntity().getShooter();
            e.getEntity().remove();
            explodeGrenade(l, gren, sh instanceof Entity s ? s : null);
            return;
        }
        if (pdc.has(BULLET, PersistentDataType.STRING)) {
            Entity bullet = e.getEntity();
            Bukkit.getScheduler().runTask(this, () -> { if (bullet.isValid()) bullet.remove(); });
            return;
        }
        Integer dmg = pdc.get(THROW, PersistentDataType.INTEGER);
        if (dmg == null) return;
        if (e.getHitEntity() instanceof LivingEntity le && isEnemy(le)) {
            Object sh = e.getEntity().getShooter();
            if (sh instanceof Entity src) le.damage(dmg, src); else le.damage(dmg);
        }
    }

    @EventHandler
    public void onLoot(LootGenerateEvent e) {
        if (e.getLootTable().getKey().getKey().startsWith("chests/") && rnd.nextInt(100) < ci("chest-chance"))
            e.getLoot().add(core(1));
    }

    @EventHandler
    public void onLoad(EntitiesLoadEvent e) {
        for (Entity en : e.getEntities()) rescan(en);
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent e) {
        Entity src = e.getEntity();
        if ("negotiator".equals(role(src))) { e.setCancelled(true); return; }
        if (isAlly(src)) {
            LivingEntity tg = e.getTarget();
            if (tg == null || !isEnemy(tg)) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Entity en = e.getRightClicked();
        Player p = e.getPlayer();
        if (en instanceof Villager v && p.isSneaking()) {
            e.setCancelled(true);
            openMilitia(p, v);
            return;
        }
        if (!"negotiator".equals(role(en))) return;
        e.setCancelled(true);
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!isCore(hand)) {
            p.sendMessage(ChatColor.GOLD + "Переговорщик: «Отдай нам Ядро!» (ПКМ с Ядром в руке)");
            return;
        }
        Base b = bases.get(baseOf(en));
        if (b == null) return;
        hand.setAmount(hand.getAmount() - 1);
        b.state = State.PEACE;
        removeMobs(b.key(), "negotiator");
        save();
        say(p.getName() + " отдал Ядро. Мобы ушли с миром... пока.");
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        Entity dead = e.getEntity();
        if (isAlly(dead)) {
            allies.remove(dead.getUniqueId());
            if (allyTeam != null) allyTeam.removeEntry(dead.getUniqueId().toString());
            e.getDrops().clear();
            e.setDroppedExp(0);
            return;
        }
        if (dead instanceof Villager v && isMilitia(v)) {
            defenders.remove(v.getUniqueId());
            emoteAround(v.getLocation(), 25, "lost", 0.6);
            return;
        }
        String r = role(dead);
        if (r == null) return;
        EntityDamageEvent last = dead.getLastDamageCause();
        if (last instanceof EntityDamageByEntityEvent ev && ev.getDamager() instanceof Projectile pr
                && pr.getShooter() instanceof Villager vs && isMilitia(vs))
            emote(vs, "kill", 0.5);
        if (r.equals("kamikaze")) {
            for (Entity p : new ArrayList<>(dead.getPassengers())) p.remove();
            bees.remove(dead.getUniqueId());
            e.getDrops().clear();
            return;
        }
        if (models.containsKey(dead.getUniqueId())) e.getDrops().clear();
        if (r.equals("negotiator")) {
            Base b = bases.get(baseOf(dead));
            if (b != null && b.state == State.NEGOTIATE) {
                startWar(b, "Переговорщика убили! На базу " + b.x + " " + b.y + " " + b.z + " идёт война!");
                save();
            }
        }
    }

    // ===== Команды =====
    String resolveKey(String k) { return ALIASES.getOrDefault(k.toLowerCase(), k); }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (!s.hasPermission("raider.admin")) { s.sendMessage("Нет прав."); return true; }
        if (a.length == 0) {
            s.sendMessage("/raider give <игрок> [предмет] [кол-во] | shop | start | stop | wave [номер] | status | clear | level <1-20> | set <ключ> <значение> | config [стр.] | reload");
            s.sendMessage("Предметы: " + String.join(", ", ITEM_IDS));
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "give" -> {
                Player t = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : (s instanceof Player p ? p : null);
                if (t == null) { s.sendMessage("Игрок не найден."); return true; }
                String id = "core"; int n = 1;
                if (a.length > 2) {
                    if (ITEM_IDS.contains(a[2].toLowerCase())) id = a[2].toLowerCase();
                    else {
                        try { n = Integer.parseInt(a[2]); }
                        catch (NumberFormatException ex) { s.sendMessage("Нет такого предмета. Список: " + String.join(", ", ITEM_IDS)); return true; }
                    }
                }
                if (a.length > 3) { try { n = Integer.parseInt(a[3]); } catch (NumberFormatException ignored) { } }
                n = Math.max(1, Math.min(64, n));
                giveItem(t, make(id, n));
                s.sendMessage("Выдано " + id + " x" + n + " игроку " + t.getName());
            }
            case "shop" -> { if (s instanceof Player p) openShop(p); else s.sendMessage("Только для игрока."); }
            case "start" -> {
                Base b = nearestBase(s);
                if (b == null) { s.sendMessage("Нет баз рядом."); return true; }
                startWar(b, "Война началась досрочно!"); save();
            }
            case "stop" -> {
                Base b = nearestBase(s);
                if (b == null) { s.sendMessage("Нет баз рядом."); return true; }
                b.state = State.PEACE; removeMobs(b.key(), null); save();
                s.sendMessage("Мир установлен, мобы убраны.");
            }
            case "wave" -> {
                Base b = nearestBase(s);
                if (b == null) { s.sendMessage("Нет баз рядом."); return true; }
                if (a.length > 1) { try { b.wave = Math.max(0, Integer.parseInt(a[1]) - 1); } catch (NumberFormatException ignored) { } }
                b.state = State.WAR; b.wave++; b.countdown = ci("timing.wave-seconds");
                spawnWave(b); save();
            }
            case "level" -> {
                if (!(s instanceof Player p) || a.length < 2) { s.sendMessage("Встань рядом с зениткой: /raider level <1-20>"); return true; }
                Turret best = null; double bd2 = 100;
                for (Turret t : turrets.values()) {
                    if (t.w() == null || !t.w().equals(p.getWorld())) continue;
                    double dd = t.top().distanceSquared(p.getLocation());
                    if (dd < bd2) { bd2 = dd; best = t; }
                }
                if (best == null) { s.sendMessage("Зениток рядом (10 блоков) нет."); return true; }
                try {
                    best.level = Math.max(1, Math.min(20, Integer.parseInt(a[1]))); best.dirt = 0; buildTurretModel(best); save();
                    s.sendMessage("Уровень зенитки: " + best.level + " «" + AA_NAMES[best.level - 1] + "»");
                } catch (NumberFormatException ex) { s.sendMessage("Это не число."); }
            }
            case "status" -> {
                if (bases.isEmpty()) s.sendMessage("Баз нет.");
                for (Base b : bases.values())
                    s.sendMessage(b.key() + " | " + b.state + " | волна " + b.wave + " | таймер " + b.countdown
                            + " | врагов " + alive(b.key()) + " | в очереди ремонта " + b.repairs.size());
                s.sendMessage("Зениток: " + turrets.size() + ", ополченцев: " + defenders.size() + ", союзников: " + allies.size());
            }
            case "clear" -> {
                for (Base b : bases.values()) removeMobs(b.key(), null);
                s.sendMessage("Все рейдеры удалены.");
            }
            case "set" -> {
                if (a.length < 3) { s.sendMessage("/raider set <ключ> <значение>. Короткие ключи: " + String.join(", ", new TreeSet<>(ALIASES.keySet()))); return true; }
                String key = resolveKey(a[1]);
                if (!getConfig().contains(key) || getConfig().isConfigurationSection(key)) { s.sendMessage("Нет такого ключа. Список: /raider config"); return true; }
                String v = a[2];
                if (v.equalsIgnoreCase("true") || v.equalsIgnoreCase("false")) getConfig().set(key, Boolean.parseBoolean(v));
                else {
                    try { getConfig().set(key, Integer.parseInt(v)); }
                    catch (NumberFormatException ex1) {
                        try { getConfig().set(key, Double.parseDouble(v)); }
                        catch (NumberFormatException ex2) { s.sendMessage("Нужно число или true/false."); return true; }
                    }
                }
                saveConfig();
                s.sendMessage("Готово: " + key + " = " + getConfig().get(key));
            }
            case "config" -> {
                int page = 1;
                if (a.length > 1) { try { page = Math.max(1, Integer.parseInt(a[1])); } catch (NumberFormatException ignored) { } }
                List<String> keys = new ArrayList<>(DESC.keySet());
                keys.remove("config-version");
                int per = 15, pages = (keys.size() + per - 1) / per;
                page = Math.min(page, pages);
                s.sendMessage(ChatColor.GOLD + "Настройки, страница " + page + "/" + pages + " (/raider config <стр.>):");
                for (int i = (page - 1) * per; i < Math.min(keys.size(), page * per); i++)
                    s.sendMessage(ChatColor.YELLOW + keys.get(i) + ChatColor.WHITE + " = " + getConfig().get(keys.get(i)) + ChatColor.GRAY + " — " + DESC.get(keys.get(i)));
            }
            case "reload" -> { reloadConfig(); s.sendMessage("Конфиг перезагружен."); }
            default -> s.sendMessage("Неизвестная подкоманда.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] a) {
        if (a.length == 1) return List.of("give", "shop", "start", "stop", "wave", "status", "clear", "level", "set", "config", "reload");
        if (a.length == 2 && a[0].equalsIgnoreCase("set")) {
            List<String> r = new ArrayList<>(new TreeSet<>(ALIASES.keySet()));
            r.addAll(DESC.keySet());
            return r;
        }
        if (a.length == 3 && a[0].equalsIgnoreCase("give")) return ITEM_IDS;
        return Collections.emptyList();
    }
}
