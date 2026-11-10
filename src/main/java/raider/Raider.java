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
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.block.BlockFace;
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
    static final Material GROUND_MAT = Material.POLISHED_BLACKSTONE;
    static final String[] GT_NAMES = {"Пулемётное гнездо", "Скорострел", "Картечница", "Бронеплощадка", "Штурмовая точка",
            "Крупнокалиберка", "Бронированный дот", "Многоствольник", "Огневая крепость", "Бастион смерти"};
    static final String[] COUNTRY_A = {"Багровых", "Железных", "Чёрных", "Громовых", "Пепельных", "Серых", "Ледяных", "Кровавых"};
    static final String[] COUNTRY_B = {"Холмов", "Клыков", "Скал", "Знамён", "Топей", "Пиков", "Границ", "Равнин"};
    static final String[] CITY_NAMES = {"Грозоград", "Каменный Брод", "Чёрная Гавань", "Вороний Приют", "Красный Форт", "Железная Падь",
            "Новый Оплот", "Пепельный Двор", "Серый Вал", "Орлиная Крепость", "Волчья Застава", "Тёмный Предел"};

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
    static final double[] WDMG = {0, 7.0, 9.0, 11.0, 0};
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
            {"helmet", "14", "1"}, {"boots", "15", "1"}, {"hedgehog", "16", "2"},
            {"rifle", "19", "1"}, {"mg", "20", "1"}, {"sniper", "21", "1"}, {"ammo", "22", "32"},
            {"grenade", "23", "4"}, {"grenade_fire", "24", "4"}, {"mine", "25", "2"},
            {"shovel_iron", "28", "1"}, {"shovel_diamond", "29", "1"}, {"shovel_netherite", "30", "1"},
            {"radio", "31", "1"}, {"gturret", "32", "1"}, {"heli", "33", "1"}, {"nuke", "34", "1"},
            {"missile_s", "37", "1"}, {"missile_m", "38", "1"}, {"missile_l", "39", "1"}};
    static final List<String> ITEM_IDS = List.of("core", "turret", "gturret", "vest_light", "vest_heavy", "vest_assault", "helmet", "boots",
            "rifle", "mg", "sniper", "ammo", "grenade", "grenade_fire", "shovel_iron", "shovel_diamond", "shovel_netherite",
            "radio", "nuke", "hedgehog", "mine", "heli", "missile_s", "missile_m", "missile_l");

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
        boolean ground = false, enemy = false;
        String sid = "";
        double acc = 0;
        Entity target;
        final List<BlockDisplay> parts = new ArrayList<>();
        Turret(String w, int x, int y, int z) { world = w; this.x = x; this.y = y; this.z = z; }
        String key() { return world + ";" + x + ";" + y + ";" + z; }
        World w() { return Bukkit.getWorld(world); }
        Location top() { return new Location(w(), x + 0.5, y + (ground ? 1.3 : 1.9 + 0.05 * level), z + 0.5); }
    }

    static final class Mdl {
        String kind; int age = 0;
        final List<BlockDisplay> parts = new ArrayList<>();
        double lx, ly, lz; float lyaw = 9999;
    }

    static final class Stuck { Location last; int bad; int fly; }

    static final class Prop {
        final String kind, world; final int x, y, z; int hp;
        final List<Entity> parts = new ArrayList<>();
        Prop(String k, String w, int x, int y, int z) { kind = k; world = w; this.x = x; this.y = y; this.z = z; }
        String key() { return world + ";" + x + ";" + y + ";" + z; }
        World w() { return Bukkit.getWorld(world); }
    }

    static final class Settlement {
        String id, name, world; int x, y, z, tier = 1, fx, fy, fz; boolean captured = false, built = false;
        World w() { return Bukkit.getWorld(world); }
    }

    static final class Heli { final List<BlockDisplay> parts = new ArrayList<>(); BlockDisplay rotorA, rotorB, tail; double rot = 0; long lastGun = 0, lastRocket = 0; }

    static final class Missile {
        World w; Location from, to; int kind; double t = 0, total, speed; UUID owner; final List<BlockDisplay> parts = new ArrayList<>();
        Location last; boolean pre;
    }

    static final class Menu implements InventoryHolder {
        final String type; final UUID target; Inventory inv;
        int mx, mz, scale = 64, tx, tz; boolean has = false; String mis = "";
        Menu(String type, UUID target) { this.type = type; this.target = target; }
        @Override public Inventory getInventory() { return inv; }
    }

    NamespacedKey CORE, ROLE, BASE, MODEL, AA, DEF, THROW, ITEM, TLEVEL, BULLET, GREN, ALLY,
            V_WEAPON, V_ARMOR, V_AIM, V_RATE, V_HP, V_REPAIR, SID, POST, HELI, PROPK;
    final Map<String, Base> bases = new HashMap<>();
    final Map<String, Turret> turrets = new HashMap<>();
    final Map<String, Prop> props = new HashMap<>();
    final Set<UUID> soldiers = new HashSet<>();
    final List<Settlement> settlements = new ArrayList<>();
    final ArrayDeque<Runnable> buildQueue = new ArrayDeque<>();
    final Map<UUID, Mdl> heldModels = new HashMap<>();
    final Set<UUID> scoped = new HashSet<>();
    final Map<UUID, ItemStack> savedHelmet = new HashMap<>();
    final Map<UUID, Heli> helis = new HashMap<>();
    final List<Missile> missiles = new ArrayList<>();
    String countryName = null;
    boolean countryDefeated = false, generating = false;
    List<LivingEntity> groundCache = new ArrayList<>();
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
        if (getConfig().getInt("config-version", 0) < 4) {
            for (String k : new ArrayList<>(getConfig().getKeys(false))) getConfig().set(k, null);
        }
        d("config-version", 4, "Служебное поле, не трогай");
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
        d("weapons.rifle-damage", 14.0, "Урон автомата (2 выстрела убивают большинство мобов)");
        d("weapons.mg-damage", 11.0, "Урон пулемёта");
        d("weapons.sniper-damage", 80.0, "Урон снайперки (убивает с 1 выстрела)");
        d("weapons.rifle-interval-ticks", 3, "Интервал выстрелов автомата");
        d("weapons.mg-interval-ticks", 2, "Интервал выстрелов пулемёта");
        d("weapons.sniper-interval-ticks", 25, "Интервал выстрелов снайперки");
        d("weapons.headshot-mult", 1.5, "Множитель урона при попадании в голову");
        d("weapons.range", 90, "Дальность стрельбы автомата и пулемёта");
        d("weapons.sniper-range", 220, "Дальность снайперки");
        d("weapons.sniper-zoom", 4, "Сила приближения прицела (уровень замедления 1..8)");
        d("weapons.spread-rifle", 0.012, "Разброс автомата");
        d("weapons.spread-mg", 0.03, "Разброс пулемёта");
        d("weapons.spread-sniper-unscoped", 0.03, "Разброс снайперки без прицела (с прицелом нет разброса)");
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
        d("prices.hedgehog", 40, "Цена: 2 противотанковых ежа");
        d("prices.mine", 60, "Цена: 2 противотанковые мины");
        d("prices.gturret", 200, "Цена: наземная турель");
        d("prices.heli", 900, "Цена: вертолёт");
        d("prices.missile_s", 500, "Цена: ракета малая");
        d("prices.missile_m", 900, "Цена: ракета средняя (кассетная)");
        d("prices.missile_l", 1800, "Цена: ракета большая (термобарическая)");
        d("hedgehog.health", 12, "Прочность ежа (взрывов до разрушения)");
        d("mine.power", 3.0, "Мощность взрыва мины");
        d("mine.damage", 40, "Урон мины обычным врагам рядом");
        d("mine.tank-damage", 90, "Урон мины танку");
        d("mine.radius", 5, "Радиус поражения мины");
        d("mine.min-health", 40, "Мина срабатывает на врагов с таким здоровьем и больше");
        d("mine.heavy-only", true, "true: только тяжёлые (танки и т.п.); false: на всех врагов");
        d("gturret.shots-base", 2.0, "Наземная турель: выстрелов/сек на 1 уровне");
        d("gturret.shots-per-level", 0.45, "Наземная турель: прирост выстрелов/сек за уровень (макс. 20)");
        d("gturret.range-base", 22, "Наземная турель: дальность на 1 уровне");
        d("gturret.range-per-level", 1.5, "Наземная турель: прирост дальности");
        d("gturret.damage-base", 3.0, "Наземная турель: урон на 1 уровне");
        d("gturret.damage-per-level", 0.7, "Наземная турель: прирост урона");
        d("gturret.upgrade-base", 8, "Наземная турель: земли на улучшение, база");
        d("gturret.upgrade-per-level", 8, "Наземная турель: плюс за уровень");
        d("missile.map-scale", 64, "Размер клетки карты ракеты (блоков) при открытии");
        d("missile.range", 4000, "Максимальная дальность пуска");
        d("missile.break-blocks", true, "Ракеты ломают блоки");
        d("missile.small-power", 8.0, "Малая ракета: мощность взрыва");
        d("missile.small-scatter", 3.0, "Малая ракета: разброс (блоков)");
        d("missile.small-speed", 2.0, "Малая ракета: скорость (блоков/тик)");
        d("missile.medium-power", 6.0, "Средняя ракета: мощность каждой боеголовки (их 6)");
        d("missile.medium-scatter", 5.0, "Средняя ракета: разброс");
        d("missile.medium-speed", 2.5, "Средняя ракета: скорость");
        d("missile.large-power", 40.0, "Большая ракета: мощность главного взрыва");
        d("missile.large-scatter", 8.0, "Большая ракета: разброс");
        d("missile.large-speed", 3.0, "Большая ракета: скорость");
        d("heli.health", 120, "Здоровье вертолёта");
        d("heli.scale", 0.3, "Размер «посадочного» существа вертолёта (если пассажиры сидят не там, подстрой)");
        d("heli.model-y", 0.0, "Сдвиг модели вертолёта по высоте");
        d("heli.max-passengers", 2, "Мест в вертолёте");
        d("heli.gun-damage", 8.0, "Урон пулемёта вертолёта (ЛКМ)");
        d("heli.gun-interval-ticks", 2, "Интервал выстрелов пулемёта вертолёта");
        d("heli.gun-range", 100, "Дальность пулемёта вертолёта");
        d("heli.rocket-power", 3.0, "Мощность ракеты вертолёта (ПКМ)");
        d("heli.rocket-interval-ticks", 30, "Перезарядка ракет вертолёта");
        d("country.enabled", true, "Вражеская страна с городами");
        d("country.cities", 3, "Сколько городов у страны");
        d("country.min-distance", 180, "Минимум блоков от базы до города");
        d("country.max-distance", 380, "Максимум блоков от базы до города");
        d("country.city-spacing", 110, "Минимальное расстояние между городами");
        d("country.max-slope", 8, "Макс. перепад высот места города");
        d("country.soldiers", 14, "Солдат в городе");
        d("country.guards", 8, "Пограничников (на башнях и воротах)");
        d("country.civilians", 10, "Гражданских в городе");
        d("country.turret-level", 4, "Уровень турелей города (растёт с номером города)");
        d("country.turret-damage-mult", 0.4, "Урон вражеских турелей по игрокам (множитель)");
        d("country.aggro-range", 32, "Дальность, с которой солдаты замечают игрока");
        d("country.capture-max-left", 0, "Сколько солдат может остаться живым для захвата флага");
        d("country.capture-reward", 400, "Награда землёй за захват города");
        d("country.garrison", 10, "Размер гарнизона (личная армия) захваченного города");
        d("country.wave-reduction-per-city", 0.15, "На сколько слабее волны за каждый захваченный город");
        d("country.peace-on-victory", true, "Мир, когда захвачены все города");
        d("country.territory-radius", 36, "Радиус захваченной территории (там не спавнятся монстры)");
        d("country.build-ms", 12, "Мс на тик при строительстве городов (меньше = меньше лагов)");
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
        SID = new NamespacedKey(this, "sid");
        POST = new NamespacedKey(this, "post");
        HELI = new NamespacedKey(this, "heli");
        PROPK = new NamespacedKey(this, "propk");
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
        Bukkit.getScheduler().runTaskTimer(this, this::buildTick, 1L, 1L);
        for (Settlement st : settlements) if (!st.built && st.w() != null) queueCity(st);
        getLogger().info("Raider v3 включён. Баз: " + bases.size() + ", зениток: " + turrets.size());
    }

    @Override
    public void onDisable() {
        for (Base b : bases.values()) if (b.bar != null) b.bar.removeAll();
        for (UUID id : new ArrayList<>(allies.keySet())) { Entity e = Bukkit.getEntity(id); if (e != null) e.remove(); }
        for (Mdl m : models.values()) for (BlockDisplay part : m.parts) part.remove();
        for (Turret t : turrets.values()) for (BlockDisplay part : t.parts) part.remove();
        for (Prop pr : props.values()) for (Entity en : pr.parts) en.remove();
        for (Heli h : helis.values()) for (BlockDisplay d : h.parts) d.remove();
        for (Missile m : missiles) for (BlockDisplay d : m.parts) d.remove();
        for (UUID id : new ArrayList<>(heldModels.keySet())) removeHeld(id);
        for (UUID id : new ArrayList<>(scoped)) { Player sp = Bukkit.getPlayer(id); if (sp != null) unscope(sp); }
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
            m.put("ground", t.ground); m.put("enemy", t.enemy); m.put("sid", t.sid);
            tl.add(m);
        }
        y.set("turrets", tl);
        List<Map<String, Object>> pl = new ArrayList<>();
        for (Prop p : props.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("kind", p.kind); m.put("world", p.world); m.put("x", p.x); m.put("y", p.y); m.put("z", p.z); m.put("hp", p.hp);
            pl.add(m);
        }
        y.set("props", pl);
        List<Map<String, Object>> sl = new ArrayList<>();
        for (Settlement s : settlements) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.id); m.put("name", s.name); m.put("world", s.world); m.put("x", s.x); m.put("y", s.y); m.put("z", s.z);
            m.put("tier", s.tier); m.put("captured", s.captured); m.put("built", s.built);
            m.put("fx", s.fx); m.put("fy", s.fy); m.put("fz", s.fz);
            sl.add(m);
        }
        y.set("settlements", sl);
        y.set("country-name", countryName);
        y.set("country-defeated", countryDefeated);
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
                if (m.get("ground") instanceof Boolean g) t.ground = g;
                if (m.get("enemy") instanceof Boolean g) t.enemy = g;
                if (m.get("sid") instanceof String g) t.sid = g;
                turrets.put(t.key(), t);
            } catch (Exception ignored) { }
        }
        for (Map<?, ?> m : y.getMapList("props")) {
            try {
                Prop p = new Prop((String) m.get("kind"), (String) m.get("world"), ((Number) m.get("x")).intValue(),
                        ((Number) m.get("y")).intValue(), ((Number) m.get("z")).intValue());
                p.hp = ((Number) m.get("hp")).intValue();
                props.put(p.key(), p);
            } catch (Exception ignored) { }
        }
        for (Map<?, ?> m : y.getMapList("settlements")) {
            try {
                Settlement s = new Settlement();
                s.id = (String) m.get("id"); s.name = (String) m.get("name"); s.world = (String) m.get("world");
                s.x = ((Number) m.get("x")).intValue(); s.y = ((Number) m.get("y")).intValue(); s.z = ((Number) m.get("z")).intValue();
                s.tier = ((Number) m.get("tier")).intValue();
                s.captured = (Boolean) m.get("captured"); s.built = (Boolean) m.get("built");
                s.fx = ((Number) m.get("fx")).intValue(); s.fy = ((Number) m.get("fy")).intValue(); s.fz = ((Number) m.get("fz")).intValue();
                settlements.add(s);
            } catch (Exception ignored) { }
        }
        countryName = y.getString("country-name");
        countryDefeated = y.getBoolean("country-defeated", false);
    }

    // ===== Утилиты =====
    String role(Entity e) { return e.getPersistentDataContainer().get(ROLE, PersistentDataType.STRING); }
    String baseOf(Entity e) { return e.getPersistentDataContainer().get(BASE, PersistentDataType.STRING); }
    String keyOf(Block b) { return b.getWorld().getName() + ";" + b.getX() + ";" + b.getY() + ";" + b.getZ(); }
    boolean isBase(Block b) { return bases.containsKey(keyOf(b)); }
    boolean isTurretBlock(Block b) { return turrets.containsKey(keyOf(b)); }
    boolean isAlly(Entity e) { return e != null && e.getPersistentDataContainer().has(ALLY, PersistentDataType.STRING); }
    boolean isEnemy(Entity e) { String r = role(e); return r != null && !r.equals("negotiator"); }
    boolean isSoldier(Entity e) { return e != null && e.getPersistentDataContainer().has(SID, PersistentDataType.STRING) && !isAlly(e); }
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
        String av = en.getPersistentDataContainer().get(ALLY, PersistentDataType.STRING);
        if (av != null && !av.equals("garrison")) en.remove();
        if (en.getPersistentDataContainer().has(SID, PersistentDataType.STRING) && en instanceof Mob) soldiers.add(en.getUniqueId());
        if (en.getPersistentDataContainer().has(HELI, PersistentDataType.STRING)) helis.putIfAbsent(en.getUniqueId(), new Heli());
        if ((en instanceof BlockDisplay || en instanceof Interaction) && en.getPersistentDataContainer().has(MODEL, PersistentDataType.STRING)) en.remove();
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

    ItemStack turretItem(int n, int level) { return turretItem(n, level, false); }

    ItemStack turretItem(int n, int level, boolean ground) {
        ItemStack it = ground
                ? mk(GROUND_MAT, n, "gturret", ChatColor.GOLD + "Наземная турель",
                null, List.of(ChatColor.GRAY + "Стреляет по наземным врагам.", ChatColor.GRAY + "ПКМ землёй по турели: улучшение до 20 уровня.",
                        ChatColor.YELLOW + "Уровень: " + level))
                : mk(TURRET_MAT, n, "turret", ChatColor.AQUA + "Зенитка",
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
            case "gturret": return turretItem(n, 1, true);
            case "hedgehog":
                return mk(Material.IRON_BARS, n, id, ChatColor.GRAY + "Противотанковый ёж", null,
                        List.of(ChatColor.GRAY + "Ставь на землю. Танки не проедут!", ChatColor.GRAY + "Забрать: Shift+ПКМ по ежу"));
            case "mine":
                return mk(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, n, id, ChatColor.DARK_GREEN + "Противотанковая мина", null,
                        List.of(ChatColor.GRAY + "Ставь на землю: взрывается под танком", ChatColor.GRAY + "Забрать: Shift+ПКМ по земле у мины"));
            case "heli":
                return mk(Material.MINECART, n, id, ChatColor.DARK_AQUA + "Вертолёт", "minecart",
                        List.of(ChatColor.GRAY + "ПКМ по земле: поставить вертолёт", ChatColor.GRAY + "ПКМ по вертолёту: сесть (до 2 человек)",
                                ChatColor.GRAY + "ЛКМ: пулемёт, ПКМ: ракета, Shift: выйти"));
            case "missile_s":
                return mk(Material.BLAZE_ROD, n, id, ChatColor.YELLOW + "Ракета «Малая»", null,
                        List.of(ChatColor.GRAY + "ПКМ: открыть карту и выбрать цель", ChatColor.GRAY + "Тактический ракетный удар"), Enchantment.UNBREAKING, 1);
            case "missile_m":
                return mk(Material.BLAZE_ROD, n, id, ChatColor.GOLD + "Ракета «Кассетная»", null,
                        List.of(ChatColor.GRAY + "ПКМ: открыть карту и выбрать цель", ChatColor.GRAY + "Шесть боеголовок по площади"), Enchantment.UNBREAKING, 1);
            case "missile_l":
                return mk(Material.BLAZE_ROD, n, id, ChatColor.DARK_RED + "Ракета «Сатана»", null,
                        List.of(ChatColor.GRAY + "ПКМ: открыть карту и выбрать цель", ChatColor.RED + "Термобарическая. Огромная воронка."), Enchantment.UNBREAKING, 3);
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
        propTick();
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
        double mult = cd("attackers.multiplier") * Math.max(0.2, 1.0 - cd("country.wave-reduction-per-city") * capturedCount());
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
        List<LivingEntity> gc = new ArrayList<>();
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
                if (air(e)) ac.add(e); else gc.add(e);
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
            if ("tank".equals(role) && now() % Math.max(2, ci("specials.tank-reload-ticks")) < 2) {
                Prop hh = nearestHedgehog(e.getLocation(), 9);
                if (hh != null && (tp == null || tp.getLocation().distance(e.getLocation()) > 22)) {
                    Vector v = new Vector(hh.x + 0.5, hh.y + 1, hh.z + 0.5).subtract(e.getEyeLocation().toVector());
                    if (v.lengthSquared() > 0) {
                        Fireball f = e.launchProjectile(Fireball.class, v.normalize());
                        f.setYield(1.8f);
                        f.setIsIncendiary(false);
                        e.getWorld().playSound(e.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.5f);
                    }
                }
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
        groundCache = gc;
        mineTick();
        soldierTick();
        heliTick();
        missileTick();
        heldTick();
        scopeCheck();
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
        if ("tank".equals(role) && nearHedgehog(e.getLocation(), 7)) { s.bad = 0; return; }
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

    // ===== Турели: зенитки (воздух) и наземные (земля), свои и вражеские =====
    int upgradeCost(Turret t) {
        String p = t.ground ? "gturret" : "turret";
        return ci(p + ".upgrade-base") + ci(p + ".upgrade-per-level") * t.level;
    }

    double turretRange(Turret t) {
        return t.ground ? cd("gturret.range-base") + cd("gturret.range-per-level") * (t.level - 1)
                : cd("turret.range-base") + cd("turret.range-per-level") * (t.level - 1);
    }

    double turretRate(Turret t) {
        return t.ground ? Math.min(20.0, cd("gturret.shots-base") + cd("gturret.shots-per-level") * (t.level - 1))
                : Math.min(20.0, cd("turret.shots-per-level") * t.level);
    }

    double turretDamage(Turret t) {
        double d = cd("gturret.damage-base") + cd("gturret.damage-per-level") * (t.level - 1);
        return t.enemy ? d * cd("country.turret-damage-mult") : d;
    }

    String tname(Turret t) { return t.ground ? GT_NAMES[Math.min(9, (t.level - 1) / 2)] : AA_NAMES[t.level - 1]; }

    Material tmat(Turret t) { return t.ground ? GROUND_MAT : TURRET_MAT; }

    Entity heliOf(Player p) {
        Entity v = p.getVehicle();
        return v != null && v.getPersistentDataContainer().has(HELI, PersistentDataType.STRING) ? v : null;
    }

    Entity pickTarget(Turret t, Location top, double range) {
        World w = t.w();
        Entity best = null; double bd2 = range * range;
        if (t.enemy) {
            for (Player p : w.getPlayers()) {
                if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;
                Entity hv = heliOf(p);
                boolean fl = hv != null || p.isGliding();
                if (t.ground == fl) continue;
                Entity cand = hv != null ? hv : p;
                double dd = cand.getLocation().distanceSquared(top);
                if (dd < bd2) { bd2 = dd; best = cand; }
            }
            return best;
        }
        Collection<? extends Entity> src = t.ground ? groundCache : airCache;
        for (Entity en : src) {
            if (en.isDead() || !en.isValid() || !en.getWorld().equals(w)) continue;
            double dd = en.getLocation().distanceSquared(top);
            if (dd < bd2) { bd2 = dd; best = en; }
        }
        return best;
    }

    void turretTick() {
        for (Turret t : new ArrayList<>(turrets.values())) {
            World w = t.w();
            if (w == null || !w.isChunkLoaded(t.x >> 4, t.z >> 4)) continue;
            if (w.getBlockAt(t.x, t.y, t.z).getType() != tmat(t)) { removeTurret(t); continue; }
            if (t.modelLevel != t.level || t.parts.isEmpty() || !t.parts.get(0).isValid()) buildTurretModel(t);
            double range = turretRange(t);
            Location top = t.top();
            if (t.enemy && nearest(top, range + 25) == null) continue;
            if (t.target == null || !t.target.isValid() || t.target.isDead() || !t.target.getWorld().equals(w)
                    || t.target.getLocation().distanceSquared(top) > range * range || c % 3 == 0) {
                t.target = pickTarget(t, top, range);
            }
            if (t.target == null) { t.acc = Math.min(t.acc, 1.0); continue; }
            t.acc += turretRate(t) / 20.0;
            int n = 0;
            while (t.acc >= 1.0 && n < 4) { fireAny(t, top, t.target); t.acc -= 1.0; n++; }
            if (t.acc > 2) t.acc = 2;
            if (n > 0) {
                if (t.ground) w.playSound(top, Sound.ENTITY_ARROW_SHOOT, 0.7f, 1.3f + t.level * 0.02f);
                else w.playSound(top, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.8f + t.level * 0.03f);
            }
        }
    }

    void fireAny(Turret t, Location top, Entity tgt) {
        if (t.ground || t.enemy) fireHit(t, top, tgt);
        else fireTurret(t, top, tgt);
    }

    void tracer(Location a, Location b) {
        World w = a.getWorld();
        Vector d = b.toVector().subtract(a.toVector());
        double len = d.length();
        if (len < 0.5) return;
        d.normalize();
        for (double s = 0; s < Math.min(len, 70); s += 1.6) {
            w.spawnParticle(Particle.CRIT, a.clone().add(d.clone().multiply(s)), 1, 0, 0, 0, 0);
        }
    }

    /** Мгновенное попадание: наземные турели и вражеские турели/зенитки. */
    void fireHit(Turret t, Location top, Entity tgt) {
        Location mz = top.clone().add((rnd.nextDouble() - 0.5) * 0.3, 0, (rnd.nextDouble() - 0.5) * 0.3);
        Location tl = tgt.getLocation().add(0, tgt.getHeight() * 0.6, 0);
        tracer(mz, tl);
        mz.getWorld().spawnParticle(Particle.FLAME, mz, 2, 0.05, 0.05, 0.05, 0.01);
        if (!t.ground) {
            tl.getWorld().spawnParticle(Particle.EXPLOSION, tl, 1, 0.2, 0.2, 0.2, 0);
            tl.getWorld().playSound(tl, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.6f, 1.2f);
        }
        if (tgt instanceof LivingEntity le) {
            le.setNoDamageTicks(0);
            le.damage(turretDamage(t));
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
        d.setViewRange(1.5f);
        if (glow) d.setGlowing(true);
        t.parts.add(d);
    }

    void buildTurretModel(Turret t) {
        World w = t.w();
        if (w == null) return;
        for (BlockDisplay part : t.parts) part.remove();
        t.parts.clear();
        Location o = new Location(w, t.x + 0.5, t.y + 1, t.z + 0.5);
        for (Entity en : w.getNearbyEntities(o, 3, 6, 3)) {
            if (en instanceof BlockDisplay && ("t:" + t.key()).equals(en.getPersistentDataContainer().get(MODEL, PersistentDataType.STRING))) en.remove();
        }
        if (t.ground) buildGround(t, o); else buildAA(t, o);
        t.modelLevel = t.level;
    }

    /** Зенитка: у каждого из 20 уровней свой облик. */
    void buildAA(Turret t, Location o) {
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
    }

    /** Наземная турель: бронеплощадка со стволами на 4 стороны, мешки с песком, щитки, радар. */
    void buildGround(Turret t, Location o) {
        int L = t.level, i = L - 1;
        Material body = AA_BODY[i];
        Material head = AA_BARREL[i];
        tpart(t, o, Material.SMOOTH_STONE, -0.5f, 0f, -0.5f, 1f, 0.12f, 1f, null, false);
        float hw = 0.62f + 0.01f * L;
        tpart(t, o, body, -hw / 2, 0.12f, -hw / 2, hw, 0.4f, hw, null, false);
        float hh = 0.28f + 0.004f * L, hd = hw * 0.85f;
        tpart(t, o, head, -hd / 2, 0.52f, -hd / 2, hd, hh, hd, null, L >= 15);
        int k = 1 + (L >= 8 ? 1 : 0) + (L >= 15 ? 1 : 0);
        float len = 0.55f + 0.035f * L, bw = 0.09f + 0.002f * L;
        float by = 0.52f + hh / 2 - bw / 2;
        float e = hw / 2 - 0.05f;
        for (int j = 0; j < k; j++) {
            float off = (float) ((j - (k - 1) / 2.0) * 0.16);
            tpart(t, o, Material.BLACK_CONCRETE, off - bw / 2, by, e, bw, bw, len, null, false);
            tpart(t, o, Material.BLACK_CONCRETE, off - bw / 2, by, -e - len, bw, bw, len, null, false);
            tpart(t, o, Material.BLACK_CONCRETE, e, by, off - bw / 2, len, bw, bw, null, false);
            tpart(t, o, Material.BLACK_CONCRETE, -e - len, by, off - bw / 2, len, bw, bw, null, false);
            if (L >= 5) {
                float mb = bw * 1.5f;
                tpart(t, o, Material.GRAY_CONCRETE, off - mb / 2, by - (mb - bw) / 2, e + len, mb, mb, 0.08f, null, false);
                tpart(t, o, Material.GRAY_CONCRETE, off - mb / 2, by - (mb - bw) / 2, -e - len - 0.08f, mb, mb, 0.08f, null, false);
                tpart(t, o, Material.GRAY_CONCRETE, e + len, by - (mb - bw) / 2, off - mb / 2, 0.08f, mb, mb, null, false);
                tpart(t, o, Material.GRAY_CONCRETE, -e - len - 0.08f, by - (mb - bw) / 2, off - mb / 2, 0.08f, mb, mb, null, false);
            }
        }
        if (L >= 2) {
            for (int sx = -1; sx <= 1; sx++) for (int sz = -1; sz <= 1; sz++) {
                if (sx == 0 && sz == 0) continue;
                tpart(t, o, Material.SANDSTONE, sx * 0.85f - 0.25f, -1f, sz * 0.85f - 0.25f, 0.5f, 0.35f, 0.5f, null, false);
            }
        }
        if (L >= 6) {
            tpart(t, o, Material.GREEN_CONCRETE, hw / 2 + 0.02f, 0.12f, 0.1f, 0.18f, 0.14f, 0.28f, null, false);
            tpart(t, o, Material.GREEN_CONCRETE, -hw / 2 - 0.2f, 0.12f, -0.38f, 0.18f, 0.14f, 0.28f, null, false);
        }
        if (L >= 10) {
            tpart(t, o, Material.GRAY_CONCRETE, hw / 2, 0.12f, -0.25f, 0.06f, 0.3f, 0.5f, null, false);
            tpart(t, o, Material.GRAY_CONCRETE, -hw / 2 - 0.06f, 0.12f, -0.25f, 0.06f, 0.3f, 0.5f, null, false);
        }
        if (L >= 12) {
            tpart(t, o, Material.LIGHTNING_ROD, -0.05f, 0.52f + hh, -0.05f, 0.1f, 0.6f, 0.1f, null, false);
            tpart(t, o, Material.TARGET, -0.2f, 0.52f + hh + 0.5f, -0.2f, 0.4f, 0.08f, 0.4f, null, true);
        }
        if (L >= 20) tpart(t, o, Material.BEACON, -0.18f, 0.52f + hh + 0.05f, -0.18f, 0.36f, 0.36f, 0.36f, null, true);
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

    // ===== Реквизит: противотанковые ежи и мины =====
    BlockDisplay bdc(Location at, Material m, String tag, double cx, double cy, double cz,
                     double sx, double sy, double sz, Quaternionf rot) {
        Vector3f tmp = new Vector3f((float) sx / 2f, (float) sy / 2f, (float) sz / 2f);
        if (rot != null) rot.transform(tmp);
        return bd(at, m, tag, cx - tmp.x, cy - tmp.y, cz - tmp.z, sx, sy, sz, rot);
    }

    Prop propFor(Block bl) {
        Prop p = props.get(keyOf(bl));
        if (p == null) {
            Prop q = props.get(keyOf(bl.getRelative(BlockFace.DOWN)));
            if (q != null && q.kind.equals("hedgehog")) p = q;
        }
        return p;
    }

    void placeProp(String kind, Block bl) {
        Prop p = new Prop(kind, bl.getWorld().getName(), bl.getX(), bl.getY(), bl.getZ());
        p.hp = kind.equals("hedgehog") ? ci("hedgehog.health") : 1;
        props.put(p.key(), p);
        buildProp(p);
        save();
    }

    void buildProp(Prop p) {
        World w = p.w();
        if (w == null) return;
        for (Entity e : p.parts) e.remove();
        p.parts.clear();
        Location o = new Location(w, p.x + 0.5, p.y, p.z + 0.5);
        String tag = "p:" + p.key();
        List<BlockDisplay> l = new ArrayList<>();
        float wid, hei;
        if (p.kind.equals("hedgehog")) {
            w.getBlockAt(p.x, p.y, p.z).setType(Material.BARRIER, false);
            w.getBlockAt(p.x, p.y + 1, p.z).setType(Material.BARRIER, false);
            double len = 2.7, th = 0.2;
            l.add(bdc(o, Material.GRAY_CONCRETE, tag, 0, 1, 0, len, th, th, new Quaternionf().rotateZ(0.62f)));
            l.add(bdc(o, Material.LIGHT_GRAY_CONCRETE, tag, 0, 1, 0, th, th, len, new Quaternionf().rotateX(0.62f)));
            l.add(bdc(o, Material.GRAY_CONCRETE, tag, 0, 1, 0, th, len, th, new Quaternionf().rotateX(0.6f).rotateZ(0.6f)));
            l.add(bdc(o, Material.BLACK_CONCRETE, tag, 0, 1, 0, 0.34, 0.34, 0.34, null));
            l.add(bdc(o, Material.ORANGE_TERRACOTTA, tag, 0, 1, 0, 0.26, 0.26, 0.26, new Quaternionf().rotateY(0.7f)));
            wid = 1.4f; hei = 2.2f;
        } else {
            l.add(bdc(o, Material.GREEN_TERRACOTTA, tag, 0, 0.07, 0, 0.9, 0.14, 0.9, null));
            l.add(bdc(o, Material.BLACK_CONCRETE, tag, 0, 0.17, 0, 0.6, 0.06, 0.6, null));
            l.add(bdc(o, Material.RED_CONCRETE, tag, 0, 0.22, 0, 0.18, 0.06, 0.18, null));
            l.add(bdc(o, Material.YELLOW_CONCRETE, tag, 0.38, 0.1, 0, 0.1, 0.1, 0.5, null));
            l.add(bdc(o, Material.YELLOW_CONCRETE, tag, -0.38, 0.1, 0, 0.1, 0.1, 0.5, null));
            wid = 1.0f; hei = 0.4f;
        }
        for (BlockDisplay d : l) { d.setViewRange(1.0f); p.parts.add(d); }
        Interaction ia = (Interaction) w.spawnEntity(o, EntityType.INTERACTION);
        ia.setInteractionWidth(wid);
        ia.setInteractionHeight(hei);
        ia.setPersistent(false);
        ia.getPersistentDataContainer().set(MODEL, PersistentDataType.STRING, tag);
        ia.getPersistentDataContainer().set(PROPK, PersistentDataType.STRING, p.key());
        p.parts.add(ia);
    }

    void removeProp(Prop p, boolean drop) {
        World w = p.w();
        for (Entity e : p.parts) e.remove();
        p.parts.clear();
        props.remove(p.key());
        if (w != null) {
            if (p.kind.equals("hedgehog")) {
                w.getBlockAt(p.x, p.y, p.z).setType(Material.AIR, false);
                w.getBlockAt(p.x, p.y + 1, p.z).setType(Material.AIR, false);
            }
            if (drop) w.dropItemNaturally(new Location(w, p.x + 0.5, p.y + 0.5, p.z + 0.5), make(p.kind, 1));
        }
        save();
    }

    void propTick() {
        for (Prop p : new ArrayList<>(props.values())) {
            World w = p.w();
            if (w == null || !w.isChunkLoaded(p.x >> 4, p.z >> 4)) continue;
            if (p.parts.isEmpty() || !p.parts.get(0).isValid()) buildProp(p);
            else if (p.kind.equals("hedgehog") && w.getBlockAt(p.x, p.y, p.z).getType() != Material.BARRIER) {
                w.getBlockAt(p.x, p.y, p.z).setType(Material.BARRIER, false);
                w.getBlockAt(p.x, p.y + 1, p.z).setType(Material.BARRIER, false);
            }
        }
    }

    Prop nearestHedgehog(Location l, double r) {
        Prop best = null; double bd2 = r * r;
        for (Prop p : props.values()) {
            if (!p.kind.equals("hedgehog") || !p.world.equals(l.getWorld().getName())) continue;
            double dx = p.x + 0.5 - l.getX(), dz = p.z + 0.5 - l.getZ();
            double dd = dx * dx + dz * dz;
            if (dd < bd2) { bd2 = dd; best = p; }
        }
        return best;
    }

    boolean nearHedgehog(Location l, double r) { return nearestHedgehog(l, r) != null; }

    /** Взрыв рядом с ежами: каждый взрыв отнимает прочность. */
    void hitHedgehogs(Location at, double r) {
        if (at.getWorld() == null) return;
        for (Prop p : new ArrayList<>(props.values())) {
            if (!p.kind.equals("hedgehog") || !p.world.equals(at.getWorld().getName())) continue;
            double dx = p.x + 0.5 - at.getX(), dy = p.y + 1 - at.getY(), dz = p.z + 0.5 - at.getZ();
            if (dx * dx + dy * dy + dz * dz > r * r) continue;
            if (--p.hp <= 0) {
                Location l = new Location(at.getWorld(), p.x + 0.5, p.y + 1, p.z + 0.5);
                l.getWorld().spawnParticle(Particle.EXPLOSION, l, 2, 0.3, 0.5, 0.3, 0);
                l.getWorld().playSound(l, Sound.BLOCK_ANVIL_DESTROY, 1f, 0.8f);
                removeProp(p, false);
            }
        }
    }

    boolean heavy(LivingEntity e) {
        if ("tank".equals(role(e))) return true;
        if (!cb("mine.heavy-only")) return true;
        AttributeInstance a = e.getAttribute(Attribute.MAX_HEALTH);
        return a != null && a.getValue() >= ci("mine.min-health");
    }

    void mineTick() {
        if (props.isEmpty() || enemyCache.isEmpty()) return;
        for (Prop p : new ArrayList<>(props.values())) {
            if (!p.kind.equals("mine")) continue;
            World w = p.w();
            if (w == null) continue;
            double mx = p.x + 0.5, mz = p.z + 0.5;
            for (LivingEntity e : enemyCache) {
                if (!e.getWorld().equals(w) || air(e)) continue;
                Location el = e.getLocation();
                double half = 0.6 + e.getWidth() / 2;
                if (Math.abs(el.getX() - mx) < half && Math.abs(el.getZ() - mz) < half && Math.abs(el.getY() - p.y) < 2.0 && heavy(e)) {
                    detonateMine(p);
                    break;
                }
            }
        }
    }

    void detonateMine(Prop p) {
        World w = p.w();
        Location l = new Location(w, p.x + 0.5, p.y + 0.3, p.z + 0.5);
        removeProp(p, false);
        w.createExplosion(l, (float) cd("mine.power"), false, false);
        double r = cd("mine.radius");
        for (Entity en : w.getNearbyEntities(l, r, r, r)) {
            if (en instanceof LivingEntity le && isEnemy(le)) {
                le.setNoDamageTicks(0);
                le.damage("tank".equals(role(le)) ? cd("mine.tank-damage") : cd("mine.damage"));
            }
        }
        w.spawnParticle(Particle.EXPLOSION, l, 3, 0.4, 0.2, 0.4, 0);
    }

    // ===== Вражеская страна: города, солдаты, захват =====
    static final Material[][] PAL = {
            {Material.DARK_OAK_LOG, Material.WHITE_TERRACOTTA, Material.SPRUCE_STAIRS, Material.SPRUCE_SLAB, Material.SPRUCE_PLANKS, Material.SPRUCE_PLANKS},
            {Material.STRIPPED_SPRUCE_LOG, Material.STONE_BRICKS, Material.DEEPSLATE_BRICK_STAIRS, Material.DEEPSLATE_BRICK_SLAB, Material.OAK_PLANKS, Material.DEEPSLATE_BRICKS},
            {Material.OAK_LOG, Material.BIRCH_PLANKS, Material.BRICK_STAIRS, Material.BRICK_SLAB, Material.OAK_PLANKS, Material.BRICKS}};

    void put(World w, int x, int y, int z, Material m) { w.getBlockAt(x, y, z).setType(m, false); }

    void putD(World w, int x, int y, int z, Material m, String data) {
        try { w.getBlockAt(x, y, z).setBlockData(Bukkit.createBlockData(m, data), false); }
        catch (Exception ex) { put(w, x, y, z, m); }
    }

    void fillB(World w, int x1, int y1, int z1, int x2, int y2, int z2, Material m) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) put(w, x, y, z, m);
    }

    Settlement settlementById(String id) {
        for (Settlement s : settlements) if (s.id.equals(id)) return s;
        return null;
    }

    int capturedCount() { int n = 0; for (Settlement s : settlements) if (s.captured) n++; return n; }

    void buildTick() {
        if (buildQueue.isEmpty()) return;
        long end = System.nanoTime() + ci("country.build-ms") * 1_000_000L;
        while (!buildQueue.isEmpty() && System.nanoTime() < end) {
            Runnable r = buildQueue.poll();
            try { r.run(); } catch (Throwable t) { getLogger().warning("Ошибка строительства: " + t); }
        }
    }

    void ensureCountry(World w, Location around) {
        if (!cb("country.enabled") || generating || countryDefeated) return;
        for (Settlement s : settlements) if (s.world.equals(w.getName())) return;
        generateCountry(w, around);
    }

    int[] findSite(World w, Location center, List<Settlement> existing) {
        int mn = ci("country.min-distance"), mx = Math.max(ci("country.max-distance"), mn + 20);
        for (int tries = 0; tries < 60; tries++) {
            double ang = rnd.nextDouble() * Math.PI * 2, dist = mn + rnd.nextDouble() * (mx - mn);
            int x = center.getBlockX() + (int) (Math.cos(ang) * dist), z = center.getBlockZ() + (int) (Math.sin(ang) * dist);
            boolean ok = true;
            for (Settlement s : existing) {
                double dx = s.x - x, dz = s.z - z;
                if (Math.sqrt(dx * dx + dz * dz) < ci("country.city-spacing")) { ok = false; break; }
            }
            if (!ok) continue;
            int lo = 999, hi = -999;
            List<Integer> hs = new ArrayList<>();
            for (int ox = -20; ox <= 20 && ok; ox += 20) for (int oz = -20; oz <= 20; oz += 20) {
                int h = w.getHighestBlockYAt(x + ox, z + oz, HeightMap.MOTION_BLOCKING_NO_LEAVES);
                Material m = w.getBlockAt(x + ox, h, z + oz).getType();
                if (m == Material.WATER || m == Material.LAVA || m == Material.ICE || m == Material.PACKED_ICE) { ok = false; break; }
                hs.add(h); lo = Math.min(lo, h); hi = Math.max(hi, h);
            }
            if (!ok || hi - lo > ci("country.max-slope")) continue;
            Collections.sort(hs);
            return new int[]{x, hs.get(hs.size() / 2), z};
        }
        return null;
    }

    void generateCountry(World w, Location center) {
        generating = true;
        countryDefeated = false;
        countryName = "Королевство " + COUNTRY_A[rnd.nextInt(COUNTRY_A.length)] + " " + COUNTRY_B[rnd.nextInt(COUNTRY_B.length)];
        List<Settlement> made = new ArrayList<>();
        List<String> names = new ArrayList<>(Arrays.asList(CITY_NAMES));
        Collections.shuffle(names, rnd);
        int n = ci("country.cities");
        for (int i = 0; i < n; i++) {
            int[] site = findSite(w, center, settlements);
            if (site == null) break;
            Settlement s = new Settlement();
            s.id = Long.toString(System.nanoTime() % 1000000007L, 36) + i;
            s.name = names.get(i % names.size());
            s.world = w.getName();
            s.x = site[0]; s.y = site[1]; s.z = site[2];
            s.tier = i + 1;
            s.fx = s.x; s.fy = s.y + 1; s.fz = s.z;
            settlements.add(s);
            made.add(s);
            queueCity(s);
        }
        buildQueue.add(() -> generating = false);
        if (made.isEmpty()) { generating = false; return; }
        say("Разведка донесла: за границей лежит " + countryName + ". Городов: " + made.size() + ". Список с координатами: /raider cities");
        save();
    }

    void queueCity(Settlement s) {
        World w = s.w();
        if (w == null) return;
        final int cx = s.x, gy = s.y, cz = s.z, R = 27;
        for (int dx = -R; dx <= R; dx++) {
            final int fdx = dx;
            buildQueue.add(() -> flattenRow(w, cx, gy, cz, fdx, R));
        }
        buildQueue.add(() -> cityRoads(w, cx, gy, cz));
        buildQueue.add(() -> cityWalls(w, cx, gy, cz));
        buildQueue.add(() -> cityHall(w, cx, gy, cz));
        int[][] lots = {{-11, -11, 1, 0, 'E'}, {11, -11, 1, 1, 'W'}, {-11, 11, 1, 2, 'E'}, {11, 11, 1, 1, 'W'},
                {-11, -18, 2, 1, 'E'}, {11, -18, 2, 2, 'W'}, {-11, 18, 2, 0, 'E'}, {11, 18, 2, 2, 'W'}};
        for (int[] lt : lots) buildQueue.add(() -> buildHouse(w, cx + lt[0], gy, cz + lt[1], lt[2], lt[3], (char) lt[4]));
        buildQueue.add(() -> cityProps(w, cx, gy, cz));
        buildQueue.add(() -> cityPeople(s));
    }

    void flattenRow(World w, int cx, int gy, int cz, int dx, int R) {
        for (int dz = -R; dz <= R; dz++) {
            int x = cx + dx, z = cz + dz;
            for (int y = gy + 16; y >= gy + 1; y--) {
                Block b = w.getBlockAt(x, y, z);
                if (!b.getType().isAir()) b.setType(Material.AIR, false);
            }
            w.getBlockAt(x, gy, z).setType(Material.GRASS_BLOCK, false);
            for (int y = gy - 1; y >= gy - 4; y--) {
                Block b = w.getBlockAt(x, y, z);
                Material m = b.getType();
                if (!m.isSolid() || m.isLiquid()) b.setType(Material.DIRT, false);
            }
        }
    }

    Material roadMat() {
        int r = rnd.nextInt(10);
        return r < 5 ? Material.GRAVEL : r < 8 ? Material.COBBLESTONE : Material.STONE_BRICKS;
    }

    void cityRoads(World w, int cx, int gy, int cz) {
        for (int i = -25; i <= 25; i++) for (int k = -1; k <= 1; k++) {
            put(w, cx + i, gy, cz + k, roadMat());
            put(w, cx + k, gy, cz + i, roadMat());
        }
        for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > 6.3) continue;
            Material m = d > 5.2 ? Material.POLISHED_BLACKSTONE_BRICKS : ((dx + dz) & 1) == 0 ? Material.POLISHED_ANDESITE : Material.STONE_BRICKS;
            put(w, cx + dx, gy, cz + dz, m);
        }
    }

    void cityWalls(World w, int cx, int gy, int cz) {
        for (int i = -22; i <= 22; i++) {
            boolean gate = Math.abs(i) <= 2;
            for (int layer = 21; layer <= 22; layer++) {
                int[][] cells = {{i, layer}, {i, -layer}, {layer, i}, {-layer, i}};
                for (int[] c2 : cells) {
                    int x = cx + c2[0], z = cz + c2[1];
                    for (int y = gy + 1; y <= gy + 6; y++) {
                        if (gate && y <= gy + 4) continue;
                        int r = rnd.nextInt(10);
                        put(w, x, y, z, y == gy + 6 ? Material.SMOOTH_STONE : r < 2 ? Material.MOSSY_STONE_BRICKS : r < 3 ? Material.CRACKED_STONE_BRICKS : Material.STONE_BRICKS);
                    }
                    if (layer == 22 && (i & 1) == 0) put(w, x, gy + 7, z, Material.STONE_BRICKS);
                }
            }
        }
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
            int tx = cx + sx * 22, tz = cz + sz * 22;
            fillB(w, tx - 2, gy + 1, tz - 2, tx + 2, gy + 9, tz + 2, Material.DEEPSLATE_BRICKS);
            fillB(w, tx - 1, gy + 1, tz - 1, tx + 1, gy + 8, tz + 1, Material.AIR);
            fillB(w, tx - 2, gy + 9, tz - 2, tx + 2, gy + 9, tz + 2, Material.POLISHED_DEEPSLATE);
            for (int a = -2; a <= 2; a++) for (int b2 = -2; b2 <= 2; b2++) {
                if (Math.abs(a) != 2 && Math.abs(b2) != 2) continue;
                if (((a + b2) & 1) == 0) put(w, tx + a, gy + 10, tz + b2, Material.DEEPSLATE_BRICK_WALL);
            }
            put(w, tx + sx * 2, gy + 11, tz + sz * 2, Material.LANTERN);
        }
        for (int g = -2; g <= 2; g += 4) {
            putD(w, cx + g, gy + 4, cz + 21, Material.LANTERN, "[hanging=true]");
            putD(w, cx + g, gy + 4, cz - 21, Material.LANTERN, "[hanging=true]");
            putD(w, cx + 21, gy + 4, cz + g, Material.LANTERN, "[hanging=true]");
            putD(w, cx - 21, gy + 4, cz + g, Material.LANTERN, "[hanging=true]");
        }
    }

    void cityHall(World w, int cx, int gy, int cz) {
        int x1 = cx - 6, x2 = cx + 6, z1 = cz - 17, z2 = cz - 9;
        fillB(w, x1, gy, z1, x2, gy, z2, Material.DARK_OAK_PLANKS);
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) {
            boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
            boolean corner = (x == x1 || x == x2) && (z == z1 || z == z2);
            for (int y = gy + 1; y <= gy + 8; y++) {
                if (edge) put(w, x, y, z, corner ? Material.DEEPSLATE_BRICKS : Material.STONE_BRICKS);
                else put(w, x, y, z, Material.AIR);
            }
            put(w, x, gy + 9, z, Material.STONE_BRICK_SLAB);
            if (edge && ((x + z) & 1) == 0) put(w, x, gy + 10, z, Material.STONE_BRICK_WALL);
        }
        for (int x = x1; x <= x2; x += 4) for (int y = gy + 1; y <= gy + 8; y++) { put(w, x, y, z1, Material.DEEPSLATE_BRICKS); put(w, x, y, z2, Material.DEEPSLATE_BRICKS); }
        for (int x = x1 + 2; x <= x2 - 2; x += 4) for (int y = gy + 3; y <= gy + 6; y++) {
            put(w, x, y, z1, Material.GLASS_PANE); put(w, x, y, z2, Material.GLASS_PANE);
        }
        for (int z = z1 + 2; z <= z2 - 2; z += 4) for (int y = gy + 3; y <= gy + 6; y++) { put(w, x1, y, z, Material.GLASS_PANE); put(w, x2, y, z, Material.GLASS_PANE); }
        fillB(w, cx - 1, gy + 1, z2, cx + 1, gy + 4, z2, Material.AIR);
        fillB(w, cx - 2, gy + 5, z2, cx + 2, gy + 5, z2, Material.DEEPSLATE_BRICKS);
        for (int z = z1 + 1; z < z2; z++) put(w, cx, gy + 1, z, Material.RED_CARPET);
        for (int x = -3; x <= 3; x += 6) for (int z = -14; z <= -11; z += 3) putD(w, cx + x, gy + 8, cz + z, Material.LANTERN, "[hanging=true]");
        put(w, cx, gy + 1, z1 + 1, Material.QUARTZ_BLOCK);
        putD(w, cx, gy + 2, z1 + 1, Material.QUARTZ_STAIRS, "[facing=south]");
    }

    void buildHouse(World w, int cx, int gy, int cz, int floors, int pal, char door) {
        Material[] P = PAL[pal];
        int H = 4 * floors;
        fillB(w, cx - 3, gy, cz - 3, cx + 3, gy, cz + 3, P[4]);
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            boolean edge = Math.abs(dx) == 3 || Math.abs(dz) == 3;
            boolean corner = Math.abs(dx) == 3 && Math.abs(dz) == 3;
            for (int y = gy + 1; y <= gy + H; y++) {
                if (edge) put(w, cx + dx, y, cz + dz, corner || (y - gy) % 4 == 0 ? P[0] : P[1]);
                else put(w, cx + dx, y, cz + dz, y == gy + H ? P[4] : Material.AIR);
            }
        }
        for (int k = 1; k < floors; k++) fillB(w, cx - 2, gy + 4 * k, cz - 2, cx + 2, gy + 4 * k, cz + 2, P[4]);
        for (int k = 0; k < floors; k++) {
            int y = gy + 4 * k + 2;
            for (int t = -1; t <= 1; t += 2) {
                put(w, cx + t, y, cz - 3, Material.GLASS_PANE); put(w, cx + t, y, cz + 3, Material.GLASS_PANE);
                put(w, cx - 3, y, cz + t, Material.GLASS_PANE); put(w, cx + 3, y, cz + t, Material.GLASS_PANE);
            }
        }
        int dx = door == 'E' ? 3 : door == 'W' ? -3 : 0, dz = door == 'S' ? 3 : door == 'N' ? -3 : 0;
        String face = door == 'E' ? "east" : door == 'W' ? "west" : door == 'S' ? "south" : "north";
        putD(w, cx + dx, gy + 1, cz + dz, Material.OAK_DOOR, "[facing=" + face + ",half=lower]");
        putD(w, cx + dx, gy + 2, cz + dz, Material.OAK_DOOR, "[facing=" + face + ",half=upper]");
        put(w, cx + dx, gy + 3, cz + dz, P[1]);
        int ox = door == 'E' ? 1 : door == 'W' ? -1 : 0;
        for (int st = 1; st <= 9; st++) put(w, cx + dx + ox * st, gy, cz + dz + (door == 'S' ? st : door == 'N' ? -st : 0), roadMat());
        // крыша: конёк вдоль оси X
        for (int k = 0; k < 4; k++) {
            int y = gy + H + 1 + k, off = 4 - k;
            for (int x = cx - 4; x <= cx + 4; x++) {
                putD(w, x, y, cz - off, P[2], "[facing=south,half=bottom]");
                putD(w, x, y, cz + off, P[2], "[facing=north,half=bottom]");
                for (int z = cz - off + 1; z <= cz + off - 1; z++) put(w, x, y, z, P[5]);
            }
        }
        for (int x = cx - 4; x <= cx + 4; x++) put(w, x, gy + H + 5, cz, P[3]);
        for (int y = gy + H + 1; y <= gy + H + 7; y++) put(w, cx - 2, y, cz + 1, Material.COBBLESTONE);
        put(w, cx - 2, gy + H + 8, cz + 1, Material.CAMPFIRE);
        put(w, cx - 2, gy + 1, cz - 2, Material.CRAFTING_TABLE);
        put(w, cx + 2, gy + 1, cz - 2, Material.BARREL);
        put(w, cx + 2, gy + 1, cz + 2, Material.LANTERN);
        putD(w, cx - 2, gy + 1, cz + 2, Material.RED_BED, "[facing=north,part=foot]");
        putD(w, cx - 2, gy + 1, cz + 1, Material.RED_BED, "[facing=north,part=head]");
    }

    void cityProps(World w, int cx, int gy, int cz) {
        // флаг: постамент, мачта, знамя
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) put(w, cx + dx, gy + 1, cz + dz, Material.STONE_BRICK_SLAB);
        put(w, cx, gy + 1, cz, Material.GILDED_BLACKSTONE);
        for (int y = gy + 2; y <= gy + 9; y++) put(w, cx, y, cz, Material.DARK_OAK_FENCE);
        put(w, cx, gy + 10, cz, Material.RED_BANNER);
        // рыночные палатки
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
            int px = cx + sx * 8, pz = cz + sz * 3;
            for (int dx = -1; dx <= 1; dx += 2) for (int dz = -1; dz <= 1; dz += 2)
                for (int y = gy + 1; y <= gy + 3; y++) put(w, px + dx, y, pz + dz, Material.OAK_FENCE);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                put(w, px + dx, gy + 4, pz + dz, ((dx + dz) & 1) == 0 ? Material.RED_WOOL : Material.WHITE_WOOL);
            put(w, px, gy + 1, pz, sx > 0 ? Material.HAY_BLOCK : Material.BARREL);
            put(w, px, gy + 2, pz, sz > 0 ? Material.MELON : Material.PUMPKIN);
        }
        // колодец
        int wx = cx + 4, wz = cz + 8;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) put(w, wx + dx, gy + 1, wz + dz, Material.COBBLESTONE);
        put(w, wx, gy + 1, wz, Material.WATER);
        for (int dx = -1; dx <= 1; dx += 2) for (int dz = -1; dz <= 1; dz += 2) for (int y = gy + 2; y <= gy + 3; y++) put(w, wx + dx, y, wz + dz, Material.OAK_FENCE);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) put(w, wx + dx, gy + 4, wz + dz, Material.SPRUCE_SLAB);
        // фонари вдоль дорог
        for (int i = -18; i <= 18; i += 6) {
            if (Math.abs(i) < 8) continue;
            for (int[] p : new int[][]{{2, i}, {-2, i}, {i, 2}, {i, -2}}) {
                if (Math.abs(p[0]) <= 6 && p[1] >= -18 && p[1] <= -8) continue;
                for (int y = gy + 1; y <= gy + 3; y++) put(w, cx + p[0], y, cz + p[1], Material.OAK_FENCE);
                put(w, cx + p[0], gy + 4, cz + p[1], Material.LANTERN);
            }
        }
        for (int[] t : new int[][]{{-18, 6}, {18, -6}, {-18, -6}, {18, 6}, {-5, 18}, {5, 18}}) {
            w.generateTree(new Location(w, cx + t[0], gy + 1, cz + t[1]), TreeType.BIRCH);
        }
    }

    void placeEnemyTurret(World w, int x, int y, int z, boolean ground, int level, Settlement s) {
        put(w, x, y, z, ground ? GROUND_MAT : TURRET_MAT);
        Turret t = new Turret(w.getName(), x, y, z);
        t.ground = ground; t.enemy = true; t.sid = s.id; t.level = Math.max(1, Math.min(20, level));
        turrets.put(t.key(), t);
    }

    LivingEntity spawnSoldier(Settlement s, Location l, EntityType type, String name, double leash, double hp, boolean friendly) {
        Entity ent = l.getWorld().spawnEntity(l, type);
        if (!(ent instanceof Mob m)) { ent.remove(); return null; }
        var pdc = m.getPersistentDataContainer();
        pdc.set(SID, PersistentDataType.STRING, s.id);
        pdc.set(POST, PersistentDataType.STRING, l.getX() + ";" + l.getY() + ";" + l.getZ() + ";" + leash);
        m.setRemoveWhenFarAway(false);
        m.setCanPickupItems(false);
        m.setCustomName((friendly ? ChatColor.BLUE : ChatColor.RED) + name);
        m.setCustomNameVisible(false);
        m.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, false, false));
        if (m instanceof Zombie z) z.setBaby(false);
        if (hp > 0) setHealth(m, hp);
        var eq = m.getEquipment();
        if (eq != null) {
            if (type == EntityType.SKELETON) eq.setItemInMainHand(new ItemStack(Material.BOW));
            else if (type == EntityType.PILLAGER) eq.setItemInMainHand(new ItemStack(Material.CROSSBOW));
            else if (type == EntityType.VINDICATOR) eq.setItemInMainHand(new ItemStack(Material.IRON_AXE));
            if (friendly) {
                eq.setHelmet(dye(new ItemStack(Material.LEATHER_HELMET), Color.BLUE));
                eq.setChestplate(dye(new ItemStack(Material.LEATHER_CHESTPLATE), Color.BLUE));
                eq.setLeggings(dye(new ItemStack(Material.LEATHER_LEGGINGS), Color.BLUE));
                eq.setBoots(dye(new ItemStack(Material.LEATHER_BOOTS), Color.BLUE));
            } else {
                eq.setHelmet(new ItemStack(rnd.nextBoolean() ? Material.IRON_HELMET : Material.CHAINMAIL_HELMET));
                eq.setChestplate(new ItemStack(rnd.nextInt(3) == 0 ? Material.IRON_CHESTPLATE : Material.CHAINMAIL_CHESTPLATE));
                if (rnd.nextBoolean()) eq.setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
            }
            eq.setHelmetDropChance(0f); eq.setChestplateDropChance(0f); eq.setLeggingsDropChance(0f);
            eq.setItemInMainHandDropChance(0f);
        }
        if (friendly) {
            pdc.set(ALLY, PersistentDataType.STRING, "garrison");
            m.setGlowing(true);
            if (allyTeam != null) allyTeam.addEntry(m.getUniqueId().toString());
        }
        soldiers.add(m.getUniqueId());
        return m;
    }

    Location roadPoint(World w, int cx, int gy, int cz) {
        for (int tries = 0; tries < 30; tries++) {
            int i = rnd.nextInt(37) - 18, k = rnd.nextInt(3) - 1;
            int dx = rnd.nextBoolean() ? i : k, dz = dx == i ? k : i;
            if (Math.abs(dx) <= 6 && dz >= -18 && dz <= -8) continue;
            return new Location(w, cx + dx + 0.5, gy + 1, cz + dz + 0.5);
        }
        return new Location(w, cx + 3.5, gy + 1, cz + 3.5);
    }

    void cityPeople(Settlement s) {
        World w = s.w();
        if (w == null) return;
        final int cx = s.x, gy = s.y, cz = s.z;
        int lvl = ci("country.turret-level") + s.tier - 1;
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2)
            placeEnemyTurret(w, cx + sx * 22, gy + 10, cz + sz * 22, true, lvl, s);
        placeEnemyTurret(w, cx - 4, gy + 10, cz - 13, true, lvl + 1, s);
        placeEnemyTurret(w, cx + 4, gy + 10, cz - 13, true, lvl + 1, s);
        placeEnemyTurret(w, cx - 16, gy + 1, cz + 4, false, lvl, s);
        placeEnemyTurret(w, cx + 16, gy + 1, cz + 4, false, lvl, s);
        EntityType[] types = {EntityType.VINDICATOR, EntityType.PILLAGER, EntityType.SKELETON, EntityType.PILLAGER};
        int n = ci("country.soldiers") + 2 * (s.tier - 1);
        for (int i = 0; i < n; i++)
            spawnSoldier(s, roadPoint(w, cx, gy, cz), types[rnd.nextInt(types.length)], "Солдат", 20 + rnd.nextInt(10), 0, false);
        int g = ci("country.guards");
        for (int i = 0; i < g; i++) {
            int sx = (i & 1) == 0 ? -1 : 1, sz = (i & 2) == 0 ? -1 : 1;
            Location l = i < 4 ? new Location(w, cx + sx * 21 + 0.5, gy + 10, cz + sz * 21 + 0.5)
                    : new Location(w, cx + (i % 2 == 0 ? 0 : (i < 6 ? 21 : -21)) + 0.5, gy + 7, cz + (i % 2 == 0 ? (i < 6 ? 21 : -21) : 0) + 0.5);
            spawnSoldier(s, l, EntityType.SKELETON, "Пограничник", 2.5, 0, false);
        }
        LivingEntity cmd = spawnSoldier(s, new Location(w, cx + 0.5, gy + 1, cz - 13.5), EntityType.VINDICATOR, "Командир гарнизона", 8, 80, false);
        if (cmd != null) {
            setScale(cmd, 1.3);
            cmd.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
            cmd.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
            cmd.getEquipment().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
            cmd.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_AXE));
        }
        Villager.Profession[] profs = {Villager.Profession.FARMER, Villager.Profession.LIBRARIAN, Villager.Profession.MASON,
                Villager.Profession.CLERIC, Villager.Profession.TOOLSMITH};
        for (int i = 0; i < ci("country.civilians"); i++) {
            Entity ent = w.spawnEntity(roadPoint(w, cx, gy, cz), EntityType.VILLAGER);
            if (ent instanceof Villager v) {
                v.setProfession(profs[rnd.nextInt(profs.length)]);
                v.setCustomName(ChatColor.GRAY + "Горожанин");
                v.getPersistentDataContainer().set(SID, PersistentDataType.STRING, s.id);
            }
        }
        s.built = true;
        save();
    }

    Settlement flagAt(Block b) {
        for (Settlement s : settlements)
            if (s.fx == b.getX() && s.fy == b.getY() && s.fz == b.getZ() && s.world.equals(b.getWorld().getName())) return s;
        return null;
    }

    int soldiersLeft(Settlement s) {
        int n = 0;
        for (UUID id : soldiers) {
            Entity en = Bukkit.getEntity(id);
            if (en != null && !en.isDead() && s.id.equals(en.getPersistentDataContainer().get(SID, PersistentDataType.STRING)) && !isAlly(en)) n++;
        }
        return n;
    }

    void tryCapture(Player p, Settlement s) {
        if (s.captured) { p.sendMessage(ChatColor.BLUE + "Город " + s.name + " уже наш."); return; }
        if (!s.built) { p.sendMessage(ChatColor.GRAY + "Город ещё строится."); return; }
        int left = soldiersLeft(s);
        if (left > ci("country.capture-max-left")) {
            p.sendMessage(ChatColor.RED + "Сначала уничтожь защитников города! Осталось солдат: " + left);
            return;
        }
        capture(s, p);
    }

    void capture(Settlement s, Player p) {
        s.captured = true;
        World w = s.w();
        if (w != null) {
            put(w, s.fx, s.fy + 9, s.fz, Material.BLUE_BANNER);
            for (Turret t : turrets.values()) if (s.id.equals(t.sid)) { t.enemy = false; t.target = null; }
            for (Entity en : w.getNearbyEntities(new Location(w, s.x, s.y, s.z), 30, 25, 30)) {
                if (en instanceof Villager v && s.id.equals(v.getPersistentDataContainer().get(SID, PersistentDataType.STRING))) {
                    enlist(v);
                    emote(v, "ally", 1.0);
                }
            }
            EntityType[] gt = {EntityType.VINDICATOR, EntityType.SKELETON, EntityType.ZOMBIE, EntityType.PILLAGER};
            for (int i = 0; i < ci("country.garrison"); i++)
                spawnSoldier(s, roadPoint(w, s.x, s.y, s.z), gt[rnd.nextInt(gt.length)], "Гарнизон", 30, 0, true);
            w.playSound(new Location(w, s.x, s.y, s.z), Sound.EVENT_RAID_HORN, SoundCategory.MASTER, 4f, 1f);
            w.spawnParticle(Particle.FIREWORK, new Location(w, s.fx + 0.5, s.fy + 10, s.fz + 0.5), 80, 2, 2, 2, 0.1);
        }
        if (p != null) giveItem(p, new ItemStack(Material.DIRT, ci("country.capture-reward")));
        say((p != null ? p.getName() : "Игроки") + " захватил(а) город " + s.name + "! Теперь это наша территория, а гарнизон — наша армия. Захвачено: "
                + capturedCount() + "/" + settlements.size());
        save();
        checkVictory();
    }

    void checkVictory() {
        if (settlements.isEmpty()) return;
        for (Settlement s : settlements) if (!s.captured) return;
        countryDefeated = true;
        say("ВСЕ города захвачены! " + countryName + " пало и больше не нападёт. Победа!");
        if (cb("country.peace-on-victory")) for (Base b : bases.values()) { b.state = State.PEACE; removeMobs(b.key(), null); }
        save();
    }

    void soldierTick() {
        if (soldiers.isEmpty() || c % 5 != 0) return;
        double aggro = cd("country.aggro-range");
        for (Iterator<UUID> it = soldiers.iterator(); it.hasNext(); ) {
            Entity en = Bukkit.getEntity(it.next());
            if (en == null) continue;
            if (!(en instanceof Mob m) || m.isDead()) { it.remove(); continue; }
            String ps = m.getPersistentDataContainer().get(POST, PersistentDataType.STRING);
            Location post = null; double leash = 25;
            if (ps != null) {
                try {
                    String[] a = ps.split(";");
                    post = new Location(m.getWorld(), Double.parseDouble(a[0]), Double.parseDouble(a[1]), Double.parseDouble(a[2]));
                    leash = Double.parseDouble(a[3]);
                } catch (Exception ignored) { }
            }
            double dpost = post == null ? 0 : m.getLocation().distance(post);
            boolean guard = leash < 4;
            if (guard && dpost > leash + 1) { m.teleport(post); continue; }
            if (isAlly(m)) {
                LivingEntity foe = nearestIn(enemyCache, m.getLocation(), 45);
                if (foe != null) { m.setTarget(foe); continue; }
                m.setTarget(null);
            } else {
                Player pl = nearest(m.getLocation(), aggro);
                if (pl != null && (post == null || pl.getLocation().distance(post) < leash + aggro)) { m.setTarget(pl); continue; }
                if (m.getTarget() instanceof Player) m.setTarget(null);
            }
            if (post != null && dpost > leash && !guard) {
                if (dpost > 45 || Math.abs(m.getLocation().getY() - post.getY()) > 4) m.teleport(post);
                else hop(m, post, 0.25);
            }
        }
    }

    void listCities(CommandSender s) {
        if (settlements.isEmpty()) { s.sendMessage(ChatColor.GRAY + "Городов пока нет. Поставь Ядро, и разведка найдёт страну."); return; }
        s.sendMessage(ChatColor.GOLD + (countryName == null ? "Страна" : countryName) + (countryDefeated ? " (ПОБЕЖДЕНА)" : "") + ":");
        int i = 1;
        for (Settlement st : settlements) {
            s.sendMessage((st.captured ? ChatColor.BLUE : ChatColor.RED) + "" + i++ + ". " + st.name + " — X " + st.x + " Y " + st.y + " Z " + st.z
                    + (st.captured ? " [наш]" : " [враг, солдат: " + soldiersLeft(st) + "]") + (st.built ? "" : " (строится)"));
        }
    }

    // ===== Карта целеуказания и баллистические ракеты =====
    void openMap(Player p, String mid) {
        Menu m = new Menu("map", null);
        m.mis = mid;
        m.mx = p.getLocation().getBlockX();
        m.mz = p.getLocation().getBlockZ();
        m.scale = Math.max(4, ci("missile.map-scale"));
        Inventory inv = Bukkit.createInventory(m, 54, ChatColor.DARK_RED + "Карта целеуказания");
        m.inv = inv;
        renderMap(inv, p, m);
        p.openInventory(inv);
    }

    void renderMap(Inventory inv, Player p, Menu m) {
        fillGlass(inv);
        World w = p.getWorld();
        int half = m.scale / 2;
        Location pl = p.getLocation();
        for (int row = 0; row < 5; row++) for (int col = 0; col < 9; col++) {
            int x = m.mx + (col - 4) * m.scale, z = m.mz + (row - 2) * m.scale;
            Material tm = Material.GRAY_STAINED_GLASS_PANE;
            String nm = ChatColor.GRAY + "Не исследовано";
            if (w.isChunkLoaded(x >> 4, z >> 4)) {
                Material t = w.getHighestBlockAt(x, z).getType();
                nm = ChatColor.GREEN + "Местность";
                if (t == Material.WATER) { tm = Material.BLUE_STAINED_GLASS_PANE; nm = ChatColor.AQUA + "Вода"; }
                else if (t == Material.LAVA) { tm = Material.ORANGE_STAINED_GLASS_PANE; nm = ChatColor.GOLD + "Лава"; }
                else if (t.isItem() && !t.isAir()) tm = t;
                else tm = Material.GRASS_BLOCK;
            }
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "X " + x + "  Z " + z);
            boolean glow = false;
            for (Base b : bases.values())
                if (b.w() != null && b.w().equals(w) && Math.abs(b.x - x) <= half && Math.abs(b.z - z) <= half) { tm = Material.BEACON; nm = ChatColor.DARK_PURPLE + "Твоя база"; }
            for (Settlement s : settlements)
                if (s.w() != null && s.w().equals(w) && Math.abs(s.x - x) <= half && Math.abs(s.z - z) <= half) {
                    tm = s.captured ? Material.BLUE_BANNER : Material.RED_BANNER;
                    nm = (s.captured ? ChatColor.BLUE : ChatColor.RED) + s.name;
                    lore.add(s.captured ? ChatColor.BLUE + "Наш город" : ChatColor.RED + "Вражеский город");
                }
            if (Math.abs(pl.getBlockX() - x) <= half && Math.abs(pl.getBlockZ() - z) <= half) { tm = Material.COMPASS; nm = ChatColor.YELLOW + "Ты здесь"; }
            if (m.has && Math.abs(m.tx - x) <= half && Math.abs(m.tz - z) <= half) { tm = Material.TNT; nm = ChatColor.DARK_RED + "" + ChatColor.BOLD + "ЦЕЛЬ"; glow = true; }
            lore.add(ChatColor.YELLOW + "Клик: выбрать цель");
            lore.add(ChatColor.YELLOW + "Shift+клик: центрировать карту");
            ItemStack it = icon(tm, nm, lore.toArray(new String[0]));
            if (glow) { ItemMeta im = it.getItemMeta(); im.setEnchantmentGlintOverride(true); it.setItemMeta(im); }
            inv.setItem(row * 9 + col, it);
        }
        inv.setItem(45, icon(Material.SPYGLASS, ChatColor.YELLOW + "Отдалить", ChatColor.GRAY + "Клетка: " + m.scale + " блоков"));
        inv.setItem(46, icon(Material.ARROW, ChatColor.YELLOW + "◄ Запад"));
        inv.setItem(47, icon(Material.ARROW, ChatColor.YELLOW + "▲ Север"));
        inv.setItem(48, icon(Material.ARROW, ChatColor.YELLOW + "▼ Юг"));
        inv.setItem(49, icon(Material.PAPER, ChatColor.GOLD + "Центр: X " + m.mx + " Z " + m.mz,
                ChatColor.GRAY + "Клетка: " + m.scale + " блоков", m.has ? ChatColor.RED + "Цель: X " + m.tx + " Z " + m.tz : ChatColor.GRAY + "Цель не выбрана"));
        inv.setItem(50, icon(Material.ARROW, ChatColor.YELLOW + "► Восток"));
        inv.setItem(51, icon(Material.SPYGLASS, ChatColor.YELLOW + "Приблизить", ChatColor.GRAY + "Точнее цель"));
        inv.setItem(52, icon(Material.COMPASS, ChatColor.YELLOW + "К себе"));
        inv.setItem(53, icon(Material.FIREWORK_ROCKET, ChatColor.RED + "" + ChatColor.BOLD + "ПУСК!",
                m.has ? ChatColor.GRAY + "Дистанция: " + (int) Math.hypot(m.tx - pl.getX(), m.tz - pl.getZ()) + " блоков" : ChatColor.GRAY + "Сначала выбери цель"));
    }

    void mapClick(Player p, Menu m, int slot, Inventory inv, boolean shift) {
        if (slot < 45) {
            int row = slot / 9, col = slot % 9;
            int x = m.mx + (col - 4) * m.scale, z = m.mz + (row - 2) * m.scale;
            if (shift) { m.mx = x; m.mz = z; } else { m.tx = x; m.tz = z; m.has = true; p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.5f); }
        } else switch (slot) {
            case 45 -> m.scale = Math.min(512, m.scale * 2);
            case 51 -> m.scale = Math.max(4, m.scale / 2);
            case 46 -> m.mx -= m.scale * 4;
            case 50 -> m.mx += m.scale * 4;
            case 47 -> m.mz -= m.scale * 2;
            case 48 -> m.mz += m.scale * 2;
            case 52 -> { m.mx = p.getLocation().getBlockX(); m.mz = p.getLocation().getBlockZ(); }
            case 53 -> { launchMissile(p, m); return; }
            default -> { }
        }
        renderMap(inv, p, m);
    }

    double mdbl(int kind, String what) {
        String k = kind == 0 ? "small" : kind == 1 ? "medium" : "large";
        return cd("missile." + k + "-" + what);
    }

    void launchMissile(Player p, Menu m) {
        if (!m.has) { p.sendMessage(ChatColor.RED + "Выбери цель на карте."); return; }
        PlayerInventory inv = p.getInventory();
        int slot = -1;
        for (int i = 0; i < 36; i++) if (m.mis.equals(itemId(inv.getItem(i)))) { slot = i; break; }
        if (slot < 0) { p.sendMessage(ChatColor.RED + "Ракеты нет в инвентаре."); p.closeInventory(); return; }
        World w = p.getWorld();
        double dist = Math.hypot(m.tx - p.getLocation().getX(), m.tz - p.getLocation().getZ());
        if (dist < 20) { p.sendMessage(ChatColor.RED + "Цель слишком близко (минимум 20 блоков)."); return; }
        if (dist > ci("missile.range")) { p.sendMessage(ChatColor.RED + "Цель вне дальности (" + ci("missile.range") + ")."); return; }
        int ty = w.getHighestBlockYAt(m.tx, m.tz);
        ItemStack st = inv.getItem(slot);
        if (st.getAmount() <= 1) inv.setItem(slot, null); else { st.setAmount(st.getAmount() - 1); inv.setItem(slot, st); }
        Missile ms = new Missile();
        ms.w = w;
        ms.from = p.getLocation().add(0, 1.5, 0);
        ms.to = new Location(w, m.tx + 0.5, ty + 1, m.tz + 0.5);
        ms.kind = m.mis.equals("missile_s") ? 0 : m.mis.equals("missile_m") ? 1 : 2;
        ms.speed = Math.max(0.5, mdbl(ms.kind, "speed"));
        ms.total = Math.max(80, dist / ms.speed);
        ms.owner = p.getUniqueId();
        missiles.add(ms);
        p.closeInventory();
        w.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.MASTER, 3f, 0.6f);
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "🚀 ПУСК РАКЕТЫ! Цель: X " + m.tx + " Z " + m.tz + ". Время полёта ~" + (int) (ms.total / 20) + " с.");
        for (Player pl : w.getPlayers())
            if (Math.hypot(pl.getLocation().getX() - m.tx, pl.getLocation().getZ() - m.tz) < 150)
                pl.sendTitle(ChatColor.DARK_RED + "⚠ РАКЕТНЫЙ УДАР", ChatColor.RED + "Покиньте зону!", 5, 60, 10);
    }

    void buildMissile(Missile m, Location at) {
        double th = new double[]{0.4, 0.55, 0.8}[m.kind], len = new double[]{2.0, 3.0, 4.5}[m.kind];
        String tag = "m:" + m.owner;
        List<BlockDisplay> l = m.parts;
        l.add(bd(at, Material.WHITE_CONCRETE, tag, -th / 2, -th / 2, -len / 2, th, th, len, null));
        l.add(bd(at, Material.BLACK_CONCRETE, tag, -th * 0.52, -th * 0.52, len * 0.05, th * 1.04, th * 1.04, 0.25, null));
        if (m.kind == 2) l.add(bd(at, Material.YELLOW_CONCRETE, tag, -th * 0.52, -th * 0.52, -len * 0.25, th * 1.04, th * 1.04, 0.3, null));
        l.add(bd(at, Material.RED_CONCRETE, tag, -th * 0.4, -th * 0.4, len / 2, th * 0.8, th * 0.8, th * 0.8, null));
        l.add(bd(at, Material.RED_CONCRETE, tag, -th * 0.2, -th * 0.2, len / 2 + th * 0.8, th * 0.4, th * 0.4, th * 0.5, null));
        l.add(bd(at, Material.GRAY_CONCRETE, tag, -th * 0.3, -th * 0.3, -len / 2 - 0.3, th * 0.6, th * 0.6, 0.3, null));
        l.add(bd(at, Material.MAGMA_BLOCK, tag, -th * 0.2, -th * 0.2, -len / 2 - 1.1, th * 0.4, th * 0.4, 0.8, null));
        l.add(bd(at, Material.BLACK_CONCRETE, tag, -th * 1.2, -0.03, -len / 2, th * 2.4, 0.06, len * 0.22, null));
        l.add(bd(at, Material.BLACK_CONCRETE, tag, -0.03, -th * 1.2, -len / 2, 0.06, th * 2.4, len * 0.22, null));
        for (BlockDisplay d : l) { d.setViewRange(4f); d.setTeleportDuration(2); }
    }

    void missileTick() {
        if (missiles.isEmpty()) return;
        for (Iterator<Missile> it = missiles.iterator(); it.hasNext(); ) {
            Missile m = it.next();
            m.t += 2;
            double s = Math.min(1.0, m.t / m.total);
            double hs = s * s * (3 - 2 * s);
            double dx = m.to.getX() - m.from.getX(), dz = m.to.getZ() - m.from.getZ();
            double hd = Math.hypot(dx, dz);
            double H = Math.max(40, Math.min(220, hd * 0.4));
            Location pos = new Location(m.w, m.from.getX() + dx * hs, m.from.getY() + (m.to.getY() - m.from.getY()) * s + H * Math.sin(Math.PI * s),
                    m.from.getZ() + dz * hs);
            Location prev = m.last == null ? m.from : m.last;
            Vector v = pos.toVector().subtract(prev.toVector());
            if (v.lengthSquared() > 1e-6) {
                float yaw = (float) Math.toDegrees(Math.atan2(-v.getX(), v.getZ()));
                float pitch = (float) -Math.toDegrees(Math.atan2(v.getY(), Math.hypot(v.getX(), v.getZ())));
                pos.setYaw(yaw);
                pos.setPitch(pitch);
            } else if (m.last != null) { pos.setYaw(m.last.getYaw()); pos.setPitch(m.last.getPitch()); }
            m.last = pos;
            if (!m.pre && m.total - m.t < 120) {
                m.pre = true;
                m.w.addPluginChunkTicket(m.to.getBlockX() >> 4, m.to.getBlockZ() >> 4, this);
                m.w.getChunkAt(m.to);
            }
            if (m.w.isChunkLoaded(pos.getBlockX() >> 4, pos.getBlockZ() >> 4)) {
                if (m.parts.isEmpty()) buildMissile(m, pos);
                for (BlockDisplay d : m.parts) if (d.isValid()) d.teleport(pos);
                Vector back = v.lengthSquared() > 0 ? v.clone().normalize().multiply(-1.5) : new Vector(0, -1, 0);
                Location tail = pos.clone().add(back);
                m.w.spawnParticle(Particle.FLAME, tail, 6, 0.15, 0.15, 0.15, 0.03);
                m.w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, tail, 2, 0.1, 0.1, 0.1, 0.01);
                if (((int) m.t) % 10 == 0) m.w.playSound(pos, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.MASTER, 3f, 0.5f);
            } else if (!m.parts.isEmpty()) {
                for (BlockDisplay d : m.parts) d.remove();
                m.parts.clear();
            }
            if (s >= 1.0) {
                for (BlockDisplay d : m.parts) d.remove();
                m.parts.clear();
                impact(m);
                m.w.removePluginChunkTicket(m.to.getBlockX() >> 4, m.to.getBlockZ() >> 4, this);
                it.remove();
            }
        }
    }

    void impact(Missile m) {
        World w = m.w;
        double sc = mdbl(m.kind, "scatter"), power = mdbl(m.kind, "power");
        Location c = m.to.clone().add((rnd.nextDouble() - 0.5) * 2 * sc, 0, (rnd.nextDouble() - 0.5) * 2 * sc);
        c.setY(w.getHighestBlockYAt(c) + 1);
        boolean br = cb("missile.break-blocks");
        if (m.kind == 0) {
            w.createExplosion(c, (float) power, false, br);
        } else if (m.kind == 1) {
            for (int i = 0; i < 6; i++) {
                final Location l = c.clone().add((rnd.nextDouble() - 0.5) * 20, 0, (rnd.nextDouble() - 0.5) * 20);
                l.setY(w.getHighestBlockYAt(l) + 1);
                Bukkit.getScheduler().runTaskLater(this, () -> w.createExplosion(l, (float) power, false, br), i * 3L);
            }
        } else {
            w.createExplosion(c, (float) power, true, br);
            w.spawnParticle(Particle.EXPLOSION_EMITTER, c, 6, 3, 2, 3, 0);
            for (int i = 0; i < 6; i++) {
                final Location l = c.clone().add((rnd.nextDouble() - 0.5) * 26, 0, (rnd.nextDouble() - 0.5) * 26);
                l.setY(w.getHighestBlockYAt(l) + 1);
                Bukkit.getScheduler().runTaskLater(this, () -> w.createExplosion(l, (float) (power * 0.35), true, br), 10L + i * 4L);
            }
        }
        for (Player pl : w.getPlayers()) pl.playSound(pl.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 4f, 0.6f);
    }

    // ===== Вертолёт =====
    void placeHeli(Player p, Block clicked, BlockFace face) {
        EntityType type = null;
        try { type = Registry.ENTITY_TYPE.get(NamespacedKey.minecraft("happy_ghast")); } catch (Throwable ignored) { }
        Material hm = Material.matchMaterial("blue_harness");
        if (type == null || hm == null) {
            p.sendMessage(ChatColor.RED + "Эта версия сервера не поддерживает вертолёт (нет счастливого гаста).");
            return;
        }
        Location at = clicked.getRelative(face).getLocation().add(0.5, 0.1, 0.5);
        at.setYaw(p.getLocation().getYaw());
        Entity en = at.getWorld().spawnEntity(at, type);
        if (!(en instanceof LivingEntity le)) { en.remove(); return; }
        le.getEquipment().setItem(EquipmentSlot.BODY, new ItemStack(hm));
        le.setInvisible(true);
        le.setRemoveWhenFarAway(false);
        AttributeInstance sc = le.getAttribute(Attribute.SCALE);
        if (sc != null) sc.setBaseValue(cd("heli.scale"));
        setHealth(le, ci("heli.health"));
        le.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, false, false));
        le.setAI(false);
        le.setCustomName(ChatColor.DARK_AQUA + "Вертолёт");
        le.setCustomNameVisible(false);
        le.getPersistentDataContainer().set(HELI, PersistentDataType.STRING, p.getUniqueId().toString());
        helis.put(le.getUniqueId(), new Heli());
        if (p.getGameMode() != GameMode.CREATIVE) {
            ItemStack h = p.getInventory().getItemInMainHand();
            h.setAmount(h.getAmount() - 1);
        }
        p.sendMessage(ChatColor.DARK_AQUA + "Вертолёт готов. ПКМ по нему: сесть. ЛКМ: пулемёт, ПКМ: ракета, Shift: выйти.");
    }

    void buildHeliModel(Heli h, LivingEntity le) {
        for (BlockDisplay d : h.parts) d.remove();
        h.parts.clear();
        Location at = le.getLocation();
        String tag = "heli:" + le.getUniqueId();
        double yo = cd("heli.model-y");
        List<BlockDisplay> l = h.parts;
        // корпус, нос, кабина
        l.add(bd(at, Material.GRAY_CONCRETE, tag, -0.8, yo + 0.55, -1.4, 1.6, 0.5, 3.0, null));
        l.add(bd(at, Material.GRAY_CONCRETE, tag, -0.6, yo + 0.6, 1.6, 1.2, 0.5, 0.7, null));
        l.add(bd(at, Material.BLACK_CONCRETE, tag, -0.35, yo + 0.65, 2.3, 0.7, 0.35, 0.4, null));
        l.add(bd(at, Material.LIGHT_BLUE_STAINED_GLASS, tag, -0.85, yo + 1.05, -0.9, 1.7, 1.3, 2.4, null));
        l.add(bd(at, Material.GRAY_CONCRETE, tag, -0.25, yo + 2.35, -0.6, 0.5, 0.35, 1.2, null));
        l.add(bd(at, Material.BLACK_CONCRETE, tag, -0.08, yo + 2.7, -0.08, 0.16, 0.3, 0.16, null));
        // хвост
        l.add(bd(at, Material.GRAY_CONCRETE, tag, -0.15, yo + 0.9, -4.6, 0.3, 0.35, 3.3, null));
        l.add(bd(at, Material.GREEN_CONCRETE, tag, -0.06, yo + 0.9, -4.9, 0.12, 1.1, 0.7, null));
        l.add(bd(at, Material.GRAY_CONCRETE, tag, -0.5, yo + 0.95, -4.3, 1.0, 0.08, 0.35, null));
        // шасси
        for (int sx = -1; sx <= 1; sx += 2) {
            l.add(bd(at, Material.BLACK_CONCRETE, tag, sx * 0.9 - 0.06, yo + 0.0, -1.3, 0.12, 0.1, 2.9, null));
            l.add(bd(at, Material.BLACK_CONCRETE, tag, sx * 0.9 - 0.04, yo + 0.1, -0.7, 0.08, 0.5, 0.08, null));
            l.add(bd(at, Material.BLACK_CONCRETE, tag, sx * 0.9 - 0.04, yo + 0.1, 0.9, 0.08, 0.5, 0.08, null));
            // пилоны с ракетами
            l.add(bd(at, Material.GRAY_CONCRETE, tag, sx * 1.1 - 0.5 * (sx > 0 ? 0 : 1) , yo + 0.9, -0.3, 0.5, 0.1, 0.4, null));
            l.add(bd(at, Material.BLACK_CONCRETE, tag, sx * 1.4 - 0.15, yo + 0.75, -0.6, 0.3, 0.3, 1.0, null));
            l.add(bd(at, Material.RED_CONCRETE, tag, sx * 1.4 - 0.1, yo + 0.8, 0.4, 0.2, 0.2, 0.25, null));
        }
        l.add(bd(at, Material.GRAY_CONCRETE, tag, -0.05, yo + 0.45, 1.9, 0.1, 0.1, 1.0, null));
        l.add(bd(at, Material.RED_CONCRETE, tag, -0.4, yo + 0.9, 0.2, 0.8, 0.05, 0.3, null));
        // винты
        h.rotorA = bd(at, Material.BLACK_CONCRETE, tag, -3, yo + 2.95, -0.15, 6.0, 0.06, 0.3, null);
        h.rotorB = bd(at, Material.BLACK_CONCRETE, tag, -0.15, yo + 2.95, -3, 0.3, 0.06, 6.0, null);
        h.tail = bd(at, Material.BLACK_CONCRETE, tag, 0.2, yo + 1.4, -4.55, 0.05, 1.0, 0.12, null);
        l.add(h.rotorA); l.add(h.rotorB); l.add(h.tail);
        for (BlockDisplay d : l) d.setViewRange(4f);
    }

    void setRotor(BlockDisplay d, double cx, double cy, double cz, double sx, double sy, double sz, Quaternionf q) {
        Vector3f tmp = new Vector3f((float) sx / 2f, (float) sy / 2f, (float) sz / 2f);
        q.transform(tmp);
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(2);
        d.setTransformation(new Transformation(new Vector3f((float) cx - tmp.x, (float) cy - tmp.y, (float) cz - tmp.z), q,
                new Vector3f((float) sx, (float) sy, (float) sz), new Quaternionf()));
    }

    void heliTick() {
        if (helis.isEmpty()) return;
        double yo = cd("heli.model-y");
        for (Iterator<Map.Entry<UUID, Heli>> it = helis.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Heli> en = it.next();
            Heli h = en.getValue();
            Entity ent = Bukkit.getEntity(en.getKey());
            if (ent == null) {
                for (BlockDisplay d : h.parts) d.remove();
                h.parts.clear();
                continue;
            }
            if (ent.isDead()) { for (BlockDisplay d : h.parts) d.remove(); it.remove(); continue; }
            if (!(ent instanceof LivingEntity le)) continue;
            boolean occ = !le.getPassengers().isEmpty();
            le.setAI(occ);
            if (h.parts.isEmpty() || !h.parts.get(0).isValid()) buildHeliModel(h, le);
            h.rot += occ ? 0.9 : 0.12;
            Location l = le.getLocation();
            l.setPitch(0f);
            for (BlockDisplay d : h.parts) if (d.isValid()) d.teleport(l);
            setRotor(h.rotorA, 0, yo + 2.98, 0, 6.0, 0.06, 0.3, new Quaternionf().rotateY((float) h.rot));
            setRotor(h.rotorB, 0, yo + 2.98, 0, 6.0, 0.06, 0.3, new Quaternionf().rotateY((float) h.rot + 1.5708f));
            setRotor(h.tail, 0.22, yo + 1.4, -4.5, 0.05, 1.0, 0.12, new Quaternionf().rotateX((float) h.rot * 1.7f));
        }
    }

    void heliFire(Player p, Entity hv, boolean gun) {
        Heli h = helis.computeIfAbsent(hv.getUniqueId(), k -> new Heli());
        long t = now();
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        if (gun) {
            if (t - h.lastGun < ci("heli.gun-interval-ticks")) return;
            h.lastGun = t;
            Vector d2 = dir.clone().add(new Vector(rnd.nextGaussian() * 0.01, rnd.nextGaussian() * 0.01, rnd.nextGaussian() * 0.01)).normalize();
            hitscan(p, eye.clone().add(dir.clone().multiply(1.0)), d2, cd("heli.gun-range"), cd("heli.gun-damage"), hv);
            p.getWorld().playSound(eye, Sound.ENTITY_SKELETON_SHOOT, 0.8f, 1.4f);
        } else {
            if (t - h.lastRocket < ci("heli.rocket-interval-ticks")) return;
            h.lastRocket = t;
            Fireball fb = p.getWorld().spawn(eye.clone().add(dir.clone().multiply(2.5)), Fireball.class);
            fb.setShooter(p);
            fb.setDirection(dir);
            fb.setVelocity(dir.clone().multiply(2.0));
            fb.setYield((float) cd("heli.rocket-power"));
            fb.setIsIncendiary(false);
            fb.getPersistentDataContainer().set(AA, PersistentDataType.BYTE, (byte) 1);
            Bukkit.getScheduler().runTaskLater(this, () -> { if (fb.isValid()) fb.remove(); }, 100L);
            p.getWorld().playSound(eye, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 0.8f);
        }
    }

    // ===== Эффект ядерного гриба =====
    void mushroom(Location c, double radius) {
        World w = c.getWorld();
        String tag = "nuke:" + System.nanoTime();
        double h = Math.max(20, radius * 1.3);
        List<BlockDisplay> l = new ArrayList<>();
        BlockDisplay stem = bd(c, Material.GRAY_CONCRETE, tag, -0.5, 0, -0.5, 1, 1, 1, null);
        BlockDisplay cap1 = bd(c, Material.ORANGE_CONCRETE, tag, -0.5, h, -0.5, 1, 1, 1, null);
        BlockDisplay cap2 = bd(c, Material.RED_CONCRETE, tag, -0.5, h + 2, -0.5, 1, 1, 1, null);
        BlockDisplay ring = bd(c, Material.BLACK_CONCRETE, tag, -0.5, h - 3, -0.5, 1, 1, 1, null);
        l.add(stem); l.add(cap1); l.add(cap2); l.add(ring);
        for (BlockDisplay d : l) { d.setViewRange(8f); d.setInterpolationDelay(0); d.setInterpolationDuration(120); d.setGlowing(true); }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            double sw = radius * 0.28;
            stem.setTransformation(new Transformation(new Vector3f((float) -sw / 2, 0, (float) -sw / 2), new Quaternionf(), new Vector3f((float) sw, (float) h, (float) sw), new Quaternionf()));
            double cw = radius * 1.1;
            cap1.setTransformation(new Transformation(new Vector3f((float) -cw / 2, (float) h, (float) -cw / 2), new Quaternionf(), new Vector3f((float) cw, (float) (radius * 0.45), (float) cw), new Quaternionf()));
            double cw2 = radius * 0.7;
            cap2.setTransformation(new Transformation(new Vector3f((float) -cw2 / 2, (float) (h + radius * 0.45), (float) -cw2 / 2), new Quaternionf(), new Vector3f((float) cw2, (float) (radius * 0.3), (float) cw2), new Quaternionf()));
            double cw3 = radius * 1.4;
            ring.setTransformation(new Transformation(new Vector3f((float) -cw3 / 2, (float) (h - radius * 0.2), (float) -cw3 / 2), new Quaternionf(), new Vector3f((float) cw3, 1.5f, (float) cw3), new Quaternionf()));
        }, 2L);
        Bukkit.getScheduler().runTaskLater(this, () -> { for (BlockDisplay d : l) d.remove(); }, 20L * 14);
        w.spawnParticle(Particle.EXPLOSION_EMITTER, c.clone().add(0, 3, 0), 5, 4, 2, 4, 0);
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
        mushroom(center, radius);
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

    // ===== Оружие игрока: мгновенное попадание, голова, снайперский прицел =====
    boolean bulletTarget(Entity en) {
        if (en instanceof Villager || en instanceof Player || en instanceof ArmorStand) return false;
        if (isAlly(en) || en.getPersistentDataContainer().has(HELI, PersistentDataType.STRING)) return false;
        if (en instanceof Tameable tm && tm.isTamed()) return false;
        return true;
    }

    /** Выстрел лучом: точно, без падения пули. Возвращает true, если попал в живое существо. */
    boolean hitscan(Player p, Location eye, Vector dir, double range, double dmg, Entity ignore) {
        World w = p.getWorld();
        RayTraceResult rr = w.rayTrace(eye, dir, range, FluidCollisionMode.NEVER, true, 0.25,
                en -> en != p && en != ignore && en instanceof LivingEntity && bulletTarget(en));
        Location end = eye.clone().add(dir.clone().multiply(range));
        boolean hit = false;
        if (rr != null) {
            end = rr.getHitPosition().toLocation(w);
            Entity he = rr.getHitEntity();
            if (he instanceof LivingEntity le) {
                double m = 1.0;
                boolean head = rr.getHitPosition().getY() >= le.getEyeLocation().getY() - 0.25 * Math.max(0.5, le.getHeight() / 1.8);
                if (head) m = cd("weapons.headshot-mult");
                le.setNoDamageTicks(0);
                le.damage(dmg * m, p);
                hit = true;
                p.sendTitle("", (head ? ChatColor.GOLD + "✖ ГОЛОВА" : ChatColor.RED + "✖"), 0, 6, 4);
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, head ? 2.0f : 1.5f);
            } else {
                w.spawnParticle(Particle.CRIT, end, 5, 0.05, 0.05, 0.05, 0.15);
            }
        }
        tracer(eye.clone().add(dir.clone().multiply(1.5)).add(0, -0.2, 0), end);
        return hit;
    }

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
        boolean sc = scoped.contains(p.getUniqueId());
        double spread = switch (id) {
            case "mg" -> cd("weapons.spread-mg");
            case "sniper" -> sc ? 0.0 : cd("weapons.spread-sniper-unscoped");
            default -> cd("weapons.spread-rifle");
        };
        double range = id.equals("sniper") ? cd("weapons.sniper-range") : cd("weapons.range");
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        if (spread > 0) dir.add(new Vector(rnd.nextGaussian() * spread, rnd.nextGaussian() * spread, rnd.nextGaussian() * spread)).normalize();
        hitscan(p, eye, dir, range, dmg, null);
        p.getWorld().playSound(p.getLocation(), id.equals("sniper") ? Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST : Sound.ENTITY_SKELETON_SHOOT,
                0.8f, id.equals("sniper") ? 0.7f : 1.9f);
    }

    void scope(Player p) {
        UUID id = p.getUniqueId();
        if (scoped.contains(id)) return;
        PlayerInventory inv = p.getInventory();
        ItemStack old = inv.getHelmet();
        if ("scope_pumpkin".equals(itemId(old))) old = null;
        savedHelmet.put(id, old == null ? new ItemStack(Material.AIR) : old);
        ItemStack pk = new ItemStack(Material.CARVED_PUMPKIN);
        ItemMeta im = pk.getItemMeta();
        im.setDisplayName(ChatColor.GRAY + "Оптический прицел");
        im.getPersistentDataContainer().set(ITEM, PersistentDataType.STRING, "scope_pumpkin");
        pk.setItemMeta(im);
        inv.setHelmet(pk);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, PotionEffect.INFINITE_DURATION,
                Math.max(0, Math.min(8, ci("weapons.sniper-zoom"))), false, false, false));
        scoped.add(id);
        p.playSound(p.getLocation(), Sound.ITEM_SPYGLASS_USE, 1f, 1f);
    }

    void unscope(Player p) {
        UUID id = p.getUniqueId();
        if (!scoped.remove(id)) return;
        ItemStack old = savedHelmet.remove(id);
        if ("scope_pumpkin".equals(itemId(p.getInventory().getHelmet()))) {
            p.getInventory().setHelmet(old == null || old.getType() == Material.AIR ? null : old);
        } else if (old != null && old.getType() != Material.AIR) {
            giveItem(p, old);
        }
        p.removePotionEffect(PotionEffectType.SLOWNESS);
        p.playSound(p.getLocation(), Sound.ITEM_SPYGLASS_STOP_USING, 1f, 1f);
    }

    void scopeCheck() {
        if (scoped.isEmpty() || c % 5 != 0) return;
        for (UUID id : new ArrayList<>(scoped)) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) { scoped.remove(id); savedHelmet.remove(id); continue; }
            if (!"sniper".equals(itemId(p.getInventory().getItemInMainHand()))) unscope(p);
        }
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

    // ===== Модели оружия в руках (видят другие игроки) =====
    String heldKind(String id) {
        return switch (id) {
            case "rifle", "mg", "sniper", "radio", "nuke" -> id;
            case "missile_s", "missile_m", "missile_l" -> "missile";
            default -> null;
        };
    }

    Mdl buildHeld(Player p, String kind) {
        Mdl md = new Mdl();
        md.kind = kind;
        Location at = p.getLocation();
        String tag = "h:" + p.getUniqueId();
        List<BlockDisplay> l = md.parts;
        double rx = -0.34;
        switch (kind) {
            case "rifle" -> {
                l.add(bd(at, Material.BLACK_CONCRETE, tag, rx - 0.035, 0.92, 0.12, 0.07, 0.13, 0.48, null));
                l.add(bd(at, Material.GRAY_CONCRETE, tag, rx - 0.03, 0.94, 0.58, 0.06, 0.08, 0.3, null));
                l.add(bd(at, Material.GRAY_CONCRETE, tag, rx - 0.0175, 0.96, 0.88, 0.035, 0.035, 0.28, null));
                l.add(bd(at, Material.BROWN_CONCRETE, tag, rx - 0.03, 0.88, -0.16, 0.06, 0.14, 0.28, null));
                l.add(bd(at, Material.BLACK_CONCRETE, tag, rx - 0.025, 0.74, 0.3, 0.05, 0.2, 0.1, null));
                l.add(bd(at, Material.GRAY_CONCRETE, tag, rx - 0.015, 1.05, 0.4, 0.03, 0.05, 0.05, null));
            }
            case "mg" -> {
                l.add(bd(at, Material.BLACK_CONCRETE, tag, rx - 0.045, 0.9, 0.05, 0.09, 0.16, 0.6, null));
                l.add(bd(at, Material.GRAY_CONCRETE, tag, rx - 0.025, 0.95, 0.65, 0.05, 0.05, 0.6, null));
                l.add(bd(at, Material.BROWN_CONCRETE, tag, rx - 0.035, 0.86, -0.25, 0.07, 0.16, 0.3, null));
                l.add(bd(at, Material.GREEN_CONCRETE, tag, rx - 0.07, 0.7, 0.2, 0.14, 0.18, 0.2, null));
                l.add(bd(at, Material.GRAY_CONCRETE, tag, rx - 0.09, 0.72, 1.0, 0.02, 0.22, 0.02, null));
                l.add(bd(at, Material.GRAY_CONCRETE, tag, rx + 0.07, 0.72, 1.0, 0.02, 0.22, 0.02, null));
            }
            case "sniper" -> {
                l.add(bd(at, Material.BLACK_CONCRETE, tag, rx - 0.03, 0.93, 0.05, 0.06, 0.11, 0.55, null));
                l.add(bd(at, Material.BLACK_CONCRETE, tag, rx - 0.02, 0.95, 0.6, 0.04, 0.04, 0.75, null));
                l.add(bd(at, Material.BROWN_CONCRETE, tag, rx - 0.03, 0.88, -0.22, 0.06, 0.14, 0.3, null));
                l.add(bd(at, Material.GRAY_CONCRETE, tag, rx - 0.03, 1.05, 0.15, 0.06, 0.06, 0.32, null));
                l.add(bd(at, Material.LIGHT_BLUE_STAINED_GLASS, tag, rx - 0.035, 1.045, 0.45, 0.07, 0.07, 0.03, null));
            }
            case "radio" -> {
                l.add(bd(at, Material.GREEN_CONCRETE, tag, -0.13, 0.9, -0.34, 0.26, 0.34, 0.14, null));
                l.add(bd(at, Material.BLACK_CONCRETE, tag, 0.07, 1.15, -0.32, 0.02, 0.6, 0.02, null));
                l.add(bd(at, Material.LIME_CONCRETE, tag, -0.08, 1.12, -0.35, 0.08, 0.05, 0.02, null));
            }
            case "nuke" -> {
                l.add(bd(at, Material.BLACK_CONCRETE, tag, rx - 0.11, 0.9, 0.25, 0.22, 0.14, 0.3, null));
                l.add(bd(at, Material.YELLOW_CONCRETE, tag, rx - 0.11, 0.9, 0.38, 0.22, 0.145, 0.05, null));
                l.add(bd(at, Material.RED_CONCRETE, tag, rx - 0.04, 1.03, 0.3, 0.08, 0.04, 0.08, null));
            }
            case "missile" -> {
                l.add(bd(at, Material.GREEN_CONCRETE, tag, rx - 0.07, 1.0, -0.35, 0.14, 0.14, 0.9, null));
                l.add(bd(at, Material.RED_CONCRETE, tag, rx - 0.05, 1.02, 0.55, 0.10, 0.10, 0.2, null));
                l.add(bd(at, Material.BLACK_CONCRETE, tag, rx - 0.09, 0.98, -0.4, 0.18, 0.18, 0.1, null));
            }
            default -> { return null; }
        }
        for (BlockDisplay d : l) { d.setViewRange(0.6f); p.hideEntity(this, d); }
        return md;
    }

    void removeHeld(UUID id) {
        Mdl md = heldModels.remove(id);
        if (md != null) for (BlockDisplay d : md.parts) d.remove();
    }

    void heldTick() {
        for (Iterator<UUID> it = heldModels.keySet().iterator(); it.hasNext(); ) {
            UUID id = it.next();
            Player p = Bukkit.getPlayer(id);
            if (p == null) {
                Mdl md = heldModels.get(id);
                if (md != null) for (BlockDisplay d : md.parts) d.remove();
                it.remove();
            }
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            String id = itemId(p.getInventory().getItemInMainHand());
            String kind = id == null ? null : heldKind(id);
            Mdl md = heldModels.get(p.getUniqueId());
            if (kind == null || p.isDead() || p.getGameMode() == GameMode.SPECTATOR) {
                if (md != null) removeHeld(p.getUniqueId());
                continue;
            }
            if (md != null && !md.kind.equals(kind)) { removeHeld(p.getUniqueId()); md = null; }
            if (md == null) {
                md = buildHeld(p, kind);
                if (md == null) continue;
                heldModels.put(p.getUniqueId(), md);
            }
            Location l = p.getLocation();
            l.setPitch(0f);
            for (BlockDisplay d : md.parts) if (d.isValid()) d.teleport(l);
        }
    }

    // ===== События =====
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        ItemStack it = e.getItemInHand();
        String iid = itemId(it);
        if (isCore(it)) {
            if (e.getBlock().getWorld().getEnvironment() != World.Environment.NORMAL) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(ChatColor.RED + "Ядро можно ставить только в обычном мире.");
                return;
            }
            Block bl = e.getBlockPlaced();
            Base b = new Base(bl.getWorld().getName(), bl.getX(), bl.getY(), bl.getZ());
            b.countdown = ci("timing.wait-seconds");
            if (countryDefeated) b.state = State.PEACE;
            bases.put(b.key(), b);
            save();
            e.getPlayer().sendMessage(ChatColor.DARK_PURPLE + "Ядро установлено. ПКМ по маяку — военный магазин. Скоро к вам придут гости...");
            ensureCountry(bl.getWorld(), bl.getLocation());
            return;
        }
        if ("turret".equals(iid) || "gturret".equals(iid)) {
            Block bl = e.getBlockPlaced();
            Turret t = new Turret(bl.getWorld().getName(), bl.getX(), bl.getY(), bl.getZ());
            t.ground = "gturret".equals(iid);
            Integer lv = it.getItemMeta().getPersistentDataContainer().get(TLEVEL, PersistentDataType.INTEGER);
            t.level = lv == null ? 1 : Math.max(1, Math.min(20, lv));
            turrets.put(t.key(), t);
            save();
            e.getPlayer().sendMessage((t.ground ? ChatColor.GOLD + "Наземная турель" : ChatColor.AQUA + "Зенитка") + " установлена (ур. " + t.level + "). ПКМ землёй — улучшить.");
            return;
        }
        if ("hedgehog".equals(iid)) {
            Block bl = e.getBlockPlaced();
            if (!bl.getRelative(BlockFace.UP).getType().isAir()) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(ChatColor.RED + "Над ежом нужно 2 блока свободного места.");
                return;
            }
            Bukkit.getScheduler().runTask(this, () -> placeProp("hedgehog", bl));
            return;
        }
        if ("mine".equals(iid)) {
            Block bl = e.getBlockPlaced();
            Bukkit.getScheduler().runTask(this, () -> { bl.setType(Material.AIR, false); placeProp("mine", bl); });
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Block blk = e.getBlock();
        if (flagAt(blk) != null) { e.setCancelled(true); return; }
        Prop pr = propFor(blk);
        if (pr != null && pr.kind.equals("hedgehog")) {
            e.setCancelled(true);
            removeProp(pr, e.getPlayer().getGameMode() != GameMode.CREATIVE);
            return;
        }
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
            if (!t.enemy) blk.getWorld().dropItemNaturally(blk.getLocation().add(0.5, 0.5, 0.5), turretItem(1, t.level, t.ground));
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
        Player p = e.getPlayer();
        Entity hv = heliOf(p);
        if (hv != null && act != Action.PHYSICAL) {
            e.setCancelled(true);
            heliFire(p, hv, act == Action.LEFT_CLICK_AIR || act == Action.LEFT_CLICK_BLOCK);
            return;
        }
        if (act != Action.RIGHT_CLICK_AIR && act != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack hand = p.getInventory().getItemInMainHand();
        String id = itemId(hand);
        if (id != null) {
            switch (id) {
                case "rifle", "mg" -> { e.setCancelled(true); shoot(p, id); return; }
                case "sniper" -> { e.setCancelled(true); if (!scoped.contains(p.getUniqueId())) scope(p); else shoot(p, id); return; }
                case "radio" -> { e.setCancelled(true); callAllies(p); return; }
                case "nuke" -> { e.setCancelled(true); startNuke(p); return; }
                case "missile_s", "missile_m", "missile_l" -> { e.setCancelled(true); openMap(p, id); return; }
                case "heli" -> {
                    e.setCancelled(true);
                    if (act == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null) placeHeli(p, e.getClickedBlock(), e.getBlockFace());
                    return;
                }
                case "grenade", "grenade_fire", "hedgehog", "mine" -> { return; }
                default -> { }
            }
        }
        if (act != Action.RIGHT_CLICK_BLOCK) return;
        Block bl = e.getClickedBlock();
        if (bl == null) return;
        Settlement fs = flagAt(bl);
        if (fs != null) { e.setCancelled(true); tryCapture(p, fs); return; }
        if (p.isSneaking() && hand.getType() == Material.AIR) {
            Prop mp = props.get(keyOf(bl.getRelative(BlockFace.UP)));
            if (mp != null && mp.kind.equals("mine")) {
                e.setCancelled(true);
                removeProp(mp, true);
                p.sendMessage(ChatColor.GRAY + "Мина снята.");
                return;
            }
        }
        Base b = bases.get(keyOf(bl));
        if (b != null) {
            if (isCore(hand)) return;
            e.setCancelled(true);
            openShop(p);
            return;
        }
        Turret t = turrets.get(keyOf(bl));
        if (t == null) return;
        if ("turret".equals(id) || "gturret".equals(id)) return;
        e.setCancelled(true);
        if (t.enemy) { p.sendMessage(ChatColor.RED + "Это вражеская турель! Захвати город, чтобы она стала твоей."); return; }
        if (isDirt(hand.getType()) && !hand.hasItemMeta()) {
            if (t.level >= 20) { p.sendMessage(ChatColor.GOLD + "Уже максимальный уровень: " + tname(t)); return; }
            int cost = upgradeCost(t);
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
                say(p.getName() + " улучшил " + (t.ground ? "наземную турель" : "зенитку") + " до уровня " + t.level + ": " + tname(t) + "!");
                emoteAround(fx, 40, "turret", 0.4);
            } else {
                p.sendMessage(ChatColor.GREEN + "Земля: " + t.dirt + "/" + cost + " до уровня " + (t.level + 1));
            }
            save();
            return;
        }
        if (hand.getType() == Material.AIR) {
            p.sendMessage((t.ground ? ChatColor.GOLD : ChatColor.AQUA) + (t.ground ? "Наземная турель" : "Зенитка") + ": ур. " + t.level + "/20 «" + tname(t) + "». Дальность "
                    + (int) turretRange(t) + " бл., скорострельность " + String.format("%.1f", turretRate(t)) + " выстр./с. "
                    + (t.level < 20 ? "Земля: " + t.dirt + "/" + upgradeCost(t) + "." : "Максимальный уровень!"));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player sp && scoped.contains(sp.getUniqueId())
                && e.getClickedInventory() instanceof PlayerInventory && e.getSlot() == 39) { e.setCancelled(true); return; }
        if (!(e.getInventory().getHolder() instanceof Menu m)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getInventory()) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;
        int slot = e.getRawSlot();
        if (m.type.equals("shop")) {
            for (String[] en : SHOP) if (Integer.parseInt(en[1]) == slot) { buy(p, en[0], Integer.parseInt(en[2]), e.getInventory()); return; }
        } else if (m.type.equals("militia")) {
            militiaClick(p, m, slot, e.getInventory());
        } else if (m.type.equals("map")) {
            mapClick(p, m, slot, e.getInventory(), e.getClick().isShiftClick());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Menu) e.setCancelled(true);
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e) {
        for (Block bl : e.getBlocks()) if (isBase(bl) || isTurretBlock(bl) || propFor(bl) != null || flagAt(bl) != null) { e.setCancelled(true); return; }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent e) {
        for (Block bl : e.getBlocks()) if (isBase(bl) || isTurretBlock(bl) || propFor(bl) != null || flagAt(bl) != null) { e.setCancelled(true); return; }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e) {
        if (e.getEntity().getPersistentDataContainer().has(AA, PersistentDataType.BYTE)) {
            e.blockList().clear();
            return;
        }
        e.blockList().removeIf(bl -> isBase(bl) || isTurretBlock(bl) || flagAt(bl) != null);
        hitHedgehogs(e.getLocation(), 4.5);
        recordRepairs(e.getLocation(), e.blockList());
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(bl -> isBase(bl) || isTurretBlock(bl) || flagAt(bl) != null);
        hitHedgehogs(e.getBlock().getLocation(), 4.5);
        recordRepairs(e.getBlock().getLocation(), e.blockList());
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (e.isSneaking() && scoped.contains(e.getPlayer().getUniqueId())) unscope(e.getPlayer());
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent e) {
        if (scoped.contains(e.getPlayer().getUniqueId())) unscope(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        unscope(e.getPlayer());
        removeHeld(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if ("scope_pumpkin".equals(itemId(p.getInventory().getHelmet()))) p.getInventory().setHelmet(null);
        if (!scoped.contains(p.getUniqueId())) p.removePotionEffect(PotionEffectType.SLOWNESS);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        ItemStack old = savedHelmet.remove(p.getUniqueId());
        if (scoped.remove(p.getUniqueId())) {
            e.getDrops().removeIf(d -> "scope_pumpkin".equals(itemId(d)));
            if (old != null && old.getType() != Material.AIR) e.getDrops().add(old);
        }
        removeHeld(p.getUniqueId());
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL || !(e.getEntity() instanceof Monster)) return;
        double r = cd("country.territory-radius");
        for (Settlement s : settlements) {
            if (!s.captured || s.w() == null || !s.w().equals(e.getLocation().getWorld())) continue;
            double dx = s.x + 0.5 - e.getLocation().getX(), dz = s.z + 0.5 - e.getLocation().getZ();
            if (dx * dx + dz * dz <= r * r) { e.setCancelled(true); return; }
        }
    }

    /** Огонь зенитки и союзные пули бьют только врагов. */
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        var pdc = e.getDamager().getPersistentDataContainer();
        Entity victim = e.getEntity();
        if (victim instanceof Interaction) { e.setCancelled(true); return; }
        if (pdc.has(AA, PersistentDataType.BYTE)) {
            if (!isEnemy(victim) && !isSoldier(victim)) e.setCancelled(true);
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
        if (src.getPersistentDataContainer().has(SID, PersistentDataType.STRING) && !isAlly(src)
                && e.getTarget() != null && !(e.getTarget() instanceof Player) && !isAlly(e.getTarget()) && !isEnemy(e.getTarget())) { e.setCancelled(true); return; }
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
        if (en.getPersistentDataContainer().has(HELI, PersistentDataType.STRING)) {
            e.setCancelled(true);
            if (p.isSneaking() && p.getInventory().getItemInMainHand().getType() == Material.AIR && en.getPassengers().isEmpty()) {
                giveItem(p, make("heli", 1));
                helis.remove(en.getUniqueId());
                en.remove();
            } else if (en.getPassengers().size() < ci("heli.max-passengers")) en.addPassenger(p);
            else p.sendMessage(ChatColor.RED + "Мест нет.");
            return;
        }
        if (en instanceof Interaction) {
            String pk = en.getPersistentDataContainer().get(PROPK, PersistentDataType.STRING);
            if (pk != null) {
                e.setCancelled(true);
                Prop pr = props.get(pk);
                if (pr != null) {
                    if (p.isSneaking()) { removeProp(pr, true); p.sendMessage(ChatColor.GRAY + "Забрал."); }
                    else p.sendMessage(ChatColor.GRAY + "Shift+ПКМ: забрать.");
                }
                return;
            }
        }
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
        if (dead.getPersistentDataContainer().has(HELI, PersistentDataType.STRING)) {
            e.getDrops().clear();
            dead.getWorld().createExplosion(dead.getLocation(), 2f, false, false);
            helis.remove(dead.getUniqueId());
            return;
        }
        if (dead.getPersistentDataContainer().has(SID, PersistentDataType.STRING)) {
            soldiers.remove(dead.getUniqueId());
            if (isAlly(dead)) { e.getDrops().clear(); e.setDroppedExp(0); }
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
        boolean pub = a.length > 0 && (a[0].equalsIgnoreCase("cities") || a[0].equalsIgnoreCase("shop"));
        if (!pub && !s.hasPermission("raider.admin")) { s.sendMessage("Нет прав."); return true; }
        if (a.length == 0) {
            s.sendMessage("/raider give <игрок> [предмет] [кол-во] | shop | start | stop | wave [номер] | status | clear | level <1-20> | set <ключ> <значение> | config [стр.] | reload | cities | country [new] | tpcity <номер> | capture <номер>");
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
                    s.sendMessage("Уровень турели: " + best.level + " «" + tname(best) + "»");
                } catch (NumberFormatException ex) { s.sendMessage("Это не число."); }
            }
            case "status" -> {
                if (bases.isEmpty()) s.sendMessage("Баз нет.");
                for (Base b : bases.values())
                    s.sendMessage(b.key() + " | " + b.state + " | волна " + b.wave + " | таймер " + b.countdown
                            + " | врагов " + alive(b.key()) + " | в очереди ремонта " + b.repairs.size());
                s.sendMessage("Турелей: " + turrets.size() + ", ополченцев: " + defenders.size() + ", союзников: " + allies.size()
                        + ", реквизита: " + props.size() + ", вертолётов: " + helis.size() + ", городов: " + settlements.size() + " (захвачено " + capturedCount() + ")");
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
            case "cities" -> listCities(s);
            case "country" -> {
                if (!(s instanceof Player p)) { s.sendMessage("Только для игрока."); return true; }
                if (a.length > 1 && a[1].equalsIgnoreCase("new")) {
                    settlements.clear();
                    countryDefeated = false;
                    generateCountry(p.getWorld(), p.getLocation());
                    s.sendMessage("Генерирую новую страну...");
                } else s.sendMessage("Страна: " + countryName + ", городов: " + settlements.size() + ", захвачено: " + capturedCount()
                        + ". Новая страна: /raider country new (старые города останутся на карте, но исчезнут из списка)");
            }
            case "tpcity" -> {
                if (!(s instanceof Player p) || a.length < 2) { s.sendMessage("/raider tpcity <номер> (игрок)"); return true; }
                try {
                    Settlement st = settlements.get(Integer.parseInt(a[1]) - 1);
                    p.teleport(new Location(st.w(), st.x + 0.5, st.y + 25, st.z + 0.5));
                } catch (Exception ex) { s.sendMessage("Нет такого города."); }
            }
            case "capture" -> {
                try {
                    Settlement st = settlements.get(Integer.parseInt(a[1]) - 1);
                    if (!st.captured) capture(st, s instanceof Player p ? p : null);
                } catch (Exception ex) { s.sendMessage("/raider capture <номер>"); }
            }
            default -> s.sendMessage("Неизвестная подкоманда.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] a) {
        if (a.length == 1) return List.of("give", "shop", "start", "stop", "wave", "status", "clear", "level", "set", "config", "reload", "cities", "country", "tpcity", "capture");
        if (a.length == 2 && a[0].equalsIgnoreCase("set")) {
            List<String> r = new ArrayList<>(new TreeSet<>(ALIASES.keySet()));
            r.addAll(DESC.keySet());
            return r;
        }
        if (a.length == 3 && a[0].equalsIgnoreCase("give")) return ITEM_IDS;
        return Collections.emptyList();
    }
}
