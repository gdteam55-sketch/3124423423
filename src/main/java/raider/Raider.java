package raider;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
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
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.File;
import java.util.*;

/**
 * Raider - плагин "Ядро Рейдера" (версия 2).
 * Ядро = база. Приходят переговорщики, потом волны врагов (боссбар сверху, сирена),
 * жители-ополченцы кидаются снежками и палками, а само ядро - зенитка (ПВО) на 20 уровней,
 * которую прокачивают землёй. Все числа настраиваются в plugins/Raider/config.yml.
 */
public final class Raider extends JavaPlugin implements Listener {

    static final Material CORE_MAT = Material.LODESTONE;

    static final EntityType[][] TIERS = {
            {EntityType.ZOMBIE, EntityType.SKELETON},
            {EntityType.SPIDER, EntityType.CREEPER, EntityType.HUSK},
            {EntityType.WITCH, EntityType.STRAY, EntityType.CAVE_SPIDER, EntityType.DROWNED, EntityType.PILLAGER},
            {EntityType.VINDICATOR, EntityType.SLIME, EntityType.BOGGED, EntityType.ZOMBIFIED_PIGLIN, EntityType.ENDERMAN},
            {EntityType.BLAZE, EntityType.WITHER_SKELETON, EntityType.MAGMA_CUBE, EntityType.PHANTOM, EntityType.BREEZE},
            {EntityType.RAVAGER, EntityType.EVOKER, EntityType.VEX, EntityType.ZOGLIN},
            {EntityType.PIGLIN_BRUTE, EntityType.SILVERFISH, EntityType.ENDERMITE, EntityType.RAVAGER, EntityType.EVOKER}
    };
    static final String[] STRANGE = {"Искажённый", "Мутант", "Пустотный", "Ржавый", "Безликий", "Древний", "Шепчущий"};

    // ===== ПВО: 20 уровней =====
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
    static final float[] SIREN = {0.6f, 0.75f, 0.9f, 1.05f, 1.2f, 1.35f, 1.5f, 1.35f, 1.2f, 1.05f, 0.9f, 0.75f};

    static final Map<String, String> ALIASES = Map.of(
            "count", "attackers.base-count",
            "perwave", "attackers.per-wave",
            "mult", "attackers.multiplier",
            "maxalive", "attackers.max-alive",
            "maxwave", "attackers.max-per-wave",
            "siren", "siren.enabled",
            "wavetime", "timing.wave-seconds",
            "villagers", "villagers.help");

    enum State { WAIT, NEGOTIATE, WAR, PEACE }

    static final class Base {
        String world; int x, y, z;
        State state = State.WAIT;
        int countdown = 30;
        int wave = 0;
        int level = 1;
        int dirt = 0;
        // временные данные (не сохраняются)
        BossBar bar;
        int waveTotal = 0;
        int alive = 0;
        int modelLevel = -1;
        Base(String w, int x, int y, int z) { world = w; this.x = x; this.y = y; this.z = z; }
        String key() { return world + ";" + x + ";" + y + ";" + z; }
        World w() { return Bukkit.getWorld(world); }
        Location loc() { return new Location(w(), x + 0.5, y, z + 0.5); }
    }

    static final class Stuck { Location last; int bad; int fly; }

    NamespacedKey CORE, ROLE, BASE, MODEL, AA, DEF, THROW;
    final Map<String, Base> bases = new HashMap<>();
    final Set<UUID> mobs = new HashSet<>();
    final Map<UUID, Integer> bees = new HashMap<>();
    final Set<UUID> defenders = new HashSet<>();
    final Map<UUID, Stuck> stuck = new HashMap<>();
    final Random rnd = new Random();
    File dataFile;
    int c = 0;
    int bombersThisWave = 0;

    // ===== Конфиг =====
    int ci(String k) { return getConfig().getInt(k); }
    double cd(String k) { return getConfig().getDouble(k); }
    boolean cb(String k) { return getConfig().getBoolean(k); }
    int minDist() { return ci("spawn.min-distance"); }
    int maxDist() { return Math.max(ci("spawn.max-distance"), minDist() + 1); }

    // ===== Жизненный цикл =====
    @Override
    public void onEnable() {
        saveDefaultConfig();
        CORE = new NamespacedKey(this, "core");
        ROLE = new NamespacedKey(this, "role");
        BASE = new NamespacedKey(this, "base");
        MODEL = new NamespacedKey(this, "model");
        AA = new NamespacedKey(this, "aa");
        DEF = new NamespacedKey(this, "defender");
        THROW = new NamespacedKey(this, "vthrow");
        getDataFolder().mkdirs();
        dataFile = new File(getDataFolder(), "data.yml");
        load();
        Bukkit.getPluginManager().registerEvents(this, this);
        for (World w : Bukkit.getWorlds()) for (Entity en : w.getEntities()) rescan(en);
        Bukkit.getScheduler().runTaskTimer(this, this::secondTick, 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(this, this::fastTick, 2L, 2L);
        getLogger().info("Raider включён. Баз: " + bases.size());
    }

    @Override
    public void onDisable() {
        for (Base b : bases.values()) if (b.bar != null) b.bar.removeAll();
        save();
    }

    // ===== Сохранение =====
    void save() {
        YamlConfiguration y = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Base b : bases.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("world", b.world); m.put("x", b.x); m.put("y", b.y); m.put("z", b.z);
            m.put("state", b.state.name()); m.put("countdown", b.countdown); m.put("wave", b.wave);
            m.put("level", b.level); m.put("dirt", b.dirt);
            list.add(m);
        }
        y.set("bases", list);
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
                if (m.get("level") instanceof Number n) b.level = Math.max(1, Math.min(20, n.intValue()));
                if (m.get("dirt") instanceof Number n) b.dirt = Math.max(0, n.intValue());
                bases.put(b.key(), b);
            } catch (Exception ignored) { }
        }
    }

    // ===== Утилиты =====
    String role(Entity e) { return e.getPersistentDataContainer().get(ROLE, PersistentDataType.STRING); }
    String baseOf(Entity e) { return e.getPersistentDataContainer().get(BASE, PersistentDataType.STRING); }
    String keyOf(Block b) { return b.getWorld().getName() + ";" + b.getX() + ";" + b.getY() + ";" + b.getZ(); }
    boolean isBase(Block b) { return bases.containsKey(keyOf(b)); }

    void say(String s) { Bukkit.broadcastMessage(ChatColor.RED + "[Raider] " + s); }

    ItemStack core(int n) {
        ItemStack it = new ItemStack(CORE_MAT, n);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(ChatColor.DARK_PURPLE + "Ядро Рейдера");
        m.setLore(List.of(ChatColor.GRAY + "Его жаждут все враждебные мобы",
                ChatColor.GRAY + "Зенитка: прокачивай землёй (ПКМ по ядру)"));
        m.setEnchantmentGlintOverride(true);
        m.getPersistentDataContainer().set(CORE, PersistentDataType.BYTE, (byte) 1);
        it.setItemMeta(m);
        return it;
    }

    boolean isCore(ItemStack it) {
        return it != null && it.hasItemMeta() && it.getItemMeta().getPersistentDataContainer().has(CORE, PersistentDataType.BYTE);
    }

    Player nearest(Location l, double r) {
        Player best = null; double bd = r * r;
        for (Player p : l.getWorld().getPlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR || p.getGameMode() == GameMode.CREATIVE) continue;
            double d = p.getLocation().distanceSquared(l);
            if (d < bd) { bd = d; best = p; }
        }
        return best;
    }

    boolean isFlyer(Entity e) {
        return e instanceof Phantom || e instanceof Vex || e instanceof Blaze || e instanceof Ghast;
    }

    boolean isFlyerType(EntityType t) {
        return t == EntityType.PHANTOM || t == EntityType.VEX || t == EntityType.BLAZE || t == EntityType.GHAST;
    }

    /** Воздушная цель для ПВО. */
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
            double d = mn + rnd.nextDouble() * (mx - mn);
            int x = b.x + (int) Math.round(Math.cos(ang) * d);
            int z = b.z + (int) Math.round(Math.sin(ang) * d);
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
                double d = p.getLocation().distanceSquared(b.loc());
                if (d < bd) { bd = d; best = b; }
            } else return b;
        }
        return best;
    }

    LivingEntity reg(LivingEntity e, String role, Base b) {
        var pdc = e.getPersistentDataContainer();
        pdc.set(ROLE, PersistentDataType.STRING, role);
        pdc.set(BASE, PersistentDataType.STRING, b.key());
        e.setRemoveWhenFarAway(false);
        e.setCanPickupItems(false);
        AttributeInstance fr = e.getAttribute(Attribute.FOLLOW_RANGE);
        if (fr != null) fr.setBaseValue(128);
        e.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, false, false));
        mobs.add(e.getUniqueId());
        return e;
    }

    void rescan(Entity en) {
        String r = role(en);
        if (r != null) {
            if (r.equals("kamikaze")) bees.putIfAbsent(en.getUniqueId(), 0);
            else if (en instanceof LivingEntity) mobs.add(en.getUniqueId());
        }
        if (en instanceof Villager && en.getPersistentDataContainer().has(DEF, PersistentDataType.STRING))
            defenders.add(en.getUniqueId());
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
            endDefenders(key);
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

    // ===== Логика баз (раз в секунду) =====
    void secondTick() {
        boolean dirty = false;
        for (Base b : new ArrayList<>(bases.values())) {
            World w = b.w();
            if (w == null || !w.isChunkLoaded(b.x >> 4, b.z >> 4)) continue;
            if (b.loc().getBlock().getType() != CORE_MAT) {
                dropBase(b); dirty = true; continue;
            }
            if (b.modelLevel != b.level) buildModel(b);
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
    }

    void dropBase(Base b) {
        bases.remove(b.key());
        removeMobs(b.key(), null);
        clearModel(b);
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
            AttributeInstance hp = p.getAttribute(Attribute.MAX_HEALTH);
            if (hp != null) { hp.setBaseValue(40); p.setHealth(40); }
            reg(p, "negotiator", b);
        }
        say("К базе " + b.x + " " + b.y + " " + b.z + " идёт группа мобов. Они требуют Ядро Рейдера! "
                + "Отдайте его (ПКМ ядром по переговорщику) за " + ci("timing.negotiate-seconds") + " сек. Если нападёте - начнётся война.");
    }

    // ===== Боссбар волны =====
    double clamp(double v) { return Math.max(0.0, Math.min(1.0, v)); }

    void updateBar(Base b) {
        World w = b.w();
        if (w == null) return;
        if (b.state != State.WAR && b.state != State.NEGOTIATE) {
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
        double r = 250;
        Location bl = b.loc();
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distanceSquared(bl) <= r * r) bar.addPlayer(p); else bar.removePlayer(p);
        }
    }

    // ===== Сирена =====
    void siren() {
        if (!cb("siren.enabled")) return;
        double r = cd("siren.radius");
        float pitch = SIREN[(c / 5) % SIREN.length];
        for (Base b : bases.values()) {
            World w = b.w();
            if (w == null || b.state != State.WAR || b.alive <= 0) continue;
            Location bl = b.loc();
            for (Player p : w.getPlayers()) {
                if (p.getLocation().distanceSquared(bl) <= r * r)
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, SoundCategory.MASTER, 2.0f, pitch);
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
        bombersThisWave = 0;

        final int total = n;
        final int kamTotal = kam;
        new BukkitRunnable() {
            int left = total, kleft = kamTotal;
            @Override public void run() {
                if (!bases.containsKey(b.key()) || b.w() == null) { cancel(); return; }
                for (int i = 0; i < 8 && left > 0; i++, left--) spawnRaider(b);
                for (int i = 0; i < 4 && kleft > 0; i++, kleft--) {
                    EntityType t = (b.wave >= 5 && rnd.nextBoolean()) ? EntityType.VEX : EntityType.BEE;
                    spawnKamikaze(b, t);
                }
                if (left <= 0 && kleft <= 0) cancel();
            }
        }.runTaskTimer(this, 0L, 2L);

        recruit(b);

        String extra = switch (wave) {
            case 4 -> " Появились автоматчики!";
            case 5 -> " Дроны-смертники в небе!";
            case 6 -> " Бомбардировщики и снайперы!";
            case 7 -> " Появились танки!";
            case 8 -> " Десант с неба!";
            case 9 -> " Появились миномёты!";
            default -> "";
        };
        say("Волна " + wave + " атакует базу! Врагов: " + b.waveTotal + "." + extra);
    }

    void spawnRaider(Base b) {
        if (!bases.containsKey(b.key()) || b.w() == null) return;
        int wave = b.wave;
        if (rnd.nextDouble() < cd("attackers.special-chance")) {
            List<String> sp = new ArrayList<>();
            if (wave >= 4) sp.add("gunner");
            if (wave >= 6) { sp.add("sniper"); if (bombersThisWave < 1 + wave / 4) sp.add("bomber"); }
            if (wave >= 7) sp.add("tank");
            if (wave >= 8) sp.add("para");
            if (wave >= 9) sp.add("mortar");
            if (!sp.isEmpty()) {
                String pick = sp.get(rnd.nextInt(sp.size()));
                if (pick.equals("para")) { spawnParatrooper(b); return; }
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
        if (wave >= 10 && rnd.nextInt(5) == 0) {
            EntityType t2 = pickType(wave);
            Entity second = b.w().spawnEntity(l, t2);
            if (second instanceof LivingEntity e2) {
                reg(e2, "raider", b);
                mutate(e2, t2, wave);
                e.addPassenger(e2);
            } else second.remove();
        }
    }

    void spawnParatrooper(Base b) {
        EntityType t = pickType(b.wave);
        if (isFlyerType(t)) t = EntityType.ZOMBIE;
        Location l = spawnPoint(b, 45);
        Entity ent = b.w().spawnEntity(l, t);
        if (!(ent instanceof LivingEntity e)) { ent.remove(); return; }
        reg(e, "raider", b);
        mutate(e, t, b.wave);
        e.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 40, 0, false, false));
    }

    void setScale(LivingEntity e, double s) {
        AttributeInstance a = e.getAttribute(Attribute.SCALE);
        if (a != null) a.setBaseValue(s);
    }

    void setHealth(LivingEntity e, double hp) {
        AttributeInstance a = e.getAttribute(Attribute.MAX_HEALTH);
        if (a != null) { a.setBaseValue(hp); e.setHealth(hp); }
    }

    void mutate(LivingEntity e, EntityType t, int wave) {
        if (wave >= 3 && e instanceof Zombie z && rnd.nextInt(5) == 0) z.setBaby(true);
        if (wave >= 4 && (e instanceof Zombie || e instanceof Skeleton) && e.getEquipment() != null && rnd.nextInt(3) == 0) {
            e.getEquipment().setHelmet(new ItemStack(wave >= 9 ? Material.DIAMOND_HELMET : Material.IRON_HELMET));
            e.getEquipment().setHelmetDropChance(0f);
        }
        if (wave >= 5 && rnd.nextInt(4) == 0) {
            setScale(e, 0.5 + rnd.nextDouble() * (wave >= 10 ? 2.5 : 1.2));
        }
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

    void spawnRole(Base b, String role) {
        if (!bases.containsKey(b.key()) || b.w() == null) return;
        World w = b.w();
        switch (role) {
            case "gunner", "sniper" -> {
                Entity ent = w.spawnEntity(spawnPoint(b, 0), EntityType.SKELETON);
                if (!(ent instanceof Skeleton s)) { ent.remove(); return; }
                var eq = s.getEquipment();
                eq.setHelmet(new ItemStack(Material.IRON_HELMET));
                eq.setItemInMainHand(new ItemStack(Material.BOW));
                eq.setHelmetDropChance(0f); eq.setItemInMainHandDropChance(0f);
                if (role.equals("gunner")) {
                    eq.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                    eq.setChestplateDropChance(0f);
                    s.setCustomName(ChatColor.GRAY + "Автоматчик");
                } else {
                    eq.setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
                    eq.setChestplateDropChance(0f);
                    s.setCustomName(ChatColor.DARK_GREEN + "Снайпер");
                }
                reg(s, role, b);
            }
            case "tank" -> {
                Entity ent = w.spawnEntity(spawnPoint(b, 0), EntityType.RAVAGER);
                if (!(ent instanceof Ravager rv)) { ent.remove(); return; }
                setScale(rv, 1.5);
                setHealth(rv, 150);
                rv.setCustomName(ChatColor.DARK_GREEN + "Танк");
                reg(rv, "tank", b);
            }
            case "mortar" -> {
                Entity ent = w.spawnEntity(spawnPoint(b, 0), EntityType.ZOMBIE);
                if (!(ent instanceof Zombie z)) { ent.remove(); return; }
                z.getEquipment().setHelmet(new ItemStack(Material.DISPENSER));
                z.getEquipment().setHelmetDropChance(0f);
                z.setCustomName(ChatColor.RED + "Миномёт");
                reg(z, "mortar", b);
            }
            case "bomber" -> {
                Entity ent = w.spawnEntity(spawnPoint(b, 22), EntityType.PHANTOM);
                if (!(ent instanceof Phantom ph)) { ent.remove(); return; }
                setScale(ph, 1.6);
                setHealth(ph, 40);
                ph.setAware(false);
                ph.setGravity(false);
                ph.setCustomName(ChatColor.DARK_GRAY + "Бомбардировщик");
                reg(ph, "bomber", b);
            }
            default -> { }
        }
    }

    void spawnKamikaze(Base b, EntityType t) {
        if (!bases.containsKey(b.key()) || b.w() == null) return;
        Location l = spawnPoint(b, 8);
        Entity ent = b.w().spawnEntity(l, t);
        if (!(ent instanceof Mob m)) { ent.remove(); return; }
        m.setRemoveWhenFarAway(false);
        m.setAware(false);
        m.setGravity(false);
        m.setCustomName((t == EntityType.VEX ? ChatColor.AQUA + "Дрон-смертник" : ChatColor.YELLOW + "Пчела-смертник"));
        if (m instanceof Vex vx) vx.setCharging(true);
        var pdc = m.getPersistentDataContainer();
        pdc.set(ROLE, PersistentDataType.STRING, "kamikaze");
        pdc.set(BASE, PersistentDataType.STRING, b.key());
        Entity tnt = b.w().spawnEntity(l, EntityType.TNT);
        if (tnt instanceof TNTPrimed tp) { tp.setFuseTicks(32767); m.addPassenger(tp); }
        bees.put(m.getUniqueId(), 0);
    }

    // ===== Жители-ополченцы =====
    void recruit(Base b) {
        if (!cb("villagers.help")) return;
        World w = b.w();
        int max = ci("villagers.max");
        int have = 0;
        for (UUID id : defenders) {
            Entity en = Bukkit.getEntity(id);
            if (en != null && b.key().equals(en.getPersistentDataContainer().get(DEF, PersistentDataType.STRING))) have++;
        }
        for (Entity en : w.getNearbyEntities(b.loc(), 80, 50, 80)) {
            if (have >= max) break;
            if (en instanceof Villager v && v.isAdult()
                    && !v.getPersistentDataContainer().has(DEF, PersistentDataType.STRING)
                    && rnd.nextDouble() < cd("villagers.chance")) {
                v.getPersistentDataContainer().set(DEF, PersistentDataType.STRING, b.key());
                defenders.add(v.getUniqueId());
                if (v.getCustomName() == null) v.setCustomName(ChatColor.GREEN + "Ополченец");
                have++;
            }
        }
    }

    void endDefenders(String key) {
        for (Iterator<UUID> it = defenders.iterator(); it.hasNext(); ) {
            Entity en = Bukkit.getEntity(it.next());
            if (en == null) continue;
            if (key.equals(en.getPersistentDataContainer().get(DEF, PersistentDataType.STRING))) {
                en.getPersistentDataContainer().remove(DEF);
                if ((ChatColor.GREEN + "Ополченец").equals(en.getCustomName())) en.setCustomName(null);
                it.remove();
            }
        }
    }

    LivingEntity nearestEnemy(Location l, double r) {
        LivingEntity best = null; double bd = r * r;
        for (UUID id : mobs) {
            Entity en = Bukkit.getEntity(id);
            if (!(en instanceof LivingEntity le) || le.isDead()) continue;
            String ro = role(le);
            if (ro == null || ro.equals("negotiator")) continue;
            if (!le.getWorld().equals(l.getWorld())) continue;
            double d = le.getLocation().distanceSquared(l);
            if (d < bd) { bd = d; best = le; }
        }
        return best;
    }

    void villagerTick() {
        for (Iterator<UUID> it = defenders.iterator(); it.hasNext(); ) {
            UUID id = it.next();
            Entity en = Bukkit.getEntity(id);
            if (en == null) continue;
            if (!(en instanceof Villager v) || v.isDead()) { it.remove(); continue; }
            String key = v.getPersistentDataContainer().get(DEF, PersistentDataType.STRING);
            Base b = key == null ? null : bases.get(key);
            if (b == null || b.state != State.WAR) {
                v.getPersistentDataContainer().remove(DEF);
                if ((ChatColor.GREEN + "Ополченец").equals(v.getCustomName())) v.setCustomName(null);
                it.remove();
                continue;
            }
            LivingEntity target = nearestEnemy(v.getLocation(), 45);
            if (target == null) continue;
            double dist = target.getLocation().distance(v.getLocation());
            if (dist <= 20 && v.hasLineOfSight(target)) {
                Vector dir = target.getEyeLocation().toVector().subtract(v.getEyeLocation().toVector());
                dir.setY(dir.getY() + dist * 0.12);
                if (dir.lengthSquared() == 0) continue;
                dir.normalize().multiply(1.2);
                Snowball sb = v.launchProjectile(Snowball.class, dir);
                int dmg = 2;
                if (rnd.nextInt(10) < 4) { sb.setItem(new ItemStack(Material.STICK)); dmg = 3; }
                sb.getPersistentDataContainer().set(THROW, PersistentDataType.INTEGER, dmg);
                v.getWorld().playSound(v.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 1f);
            } else if (dist > 16) {
                hop(v, target.getLocation(), 0.18);
            }
        }
    }

    // ===== Быстрый цикл (каждые 2 тика) =====
    void fastTick() {
        c++;
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

            if ("bomber".equals(role)) { bomberTick(e, b); continue; }

            Stuck s = stuck.computeIfAbsent(id, k -> new Stuck());
            Player tp = nearest(e.getLocation(), 40);
            Location dest = (tp != null && !"negotiator".equals(role)) ? tp.getLocation() : bl;

            if (s.fly > 0) { fly(e, s, dest); continue; }
            if (c % 10 == 0) stuckCheck(e, s, dest, role);
            if (c % 8 == 0) steer(e, role, bl, tp);

            if ("gunner".equals(role) && c % 2 == 0 && tp != null
                    && tp.getLocation().distance(e.getLocation()) < 30 && e.hasLineOfSight(tp)) {
                shootArrow(e, tp, 2.4, 2.0, 0.04);
            }
            if ("sniper".equals(role) && c % 40 == 0) {
                Player sp = nearest(e.getLocation(), 70);
                if (sp != null && e.hasLineOfSight(sp)) shootArrow(e, sp, 4.0, 9.0, 0.0);
            }
            if ("tank".equals(role) && c % 30 == 0 && tp != null
                    && tp.getLocation().distance(e.getLocation()) < 40 && e.hasLineOfSight(tp)) {
                Vector v = tp.getEyeLocation().toVector().subtract(e.getEyeLocation().toVector()).normalize();
                Fireball f = e.launchProjectile(Fireball.class, v);
                f.setYield(1.8f);
                f.setIsIncendiary(false);
                e.getWorld().playSound(e.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.5f);
            }
            if ("mortar".equals(role) && c % 50 == 0) {
                Player mp = nearest(e.getLocation(), 70);
                Location target = mp != null ? mp.getLocation() : bl;
                double d = target.distance(e.getLocation());
                if (d >= 15 && d <= 75) {
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
        tickBees();
        for (Base b : new ArrayList<>(bases.values())) turret(b);
        if (c % 8 == 0) villagerTick();
        if (c % 5 == 0) siren();
    }

    void shootArrow(LivingEntity e, Player target, double speed, double damage, double spread) {
        Location eye = e.getEyeLocation();
        Vector v = target.getEyeLocation().toVector().subtract(eye.toVector());
        if (v.lengthSquared() == 0) return;
        v.normalize();
        if (spread > 0) v.add(new Vector(rnd.nextGaussian() * spread, rnd.nextGaussian() * spread, rnd.nextGaussian() * spread));
        v.multiply(speed);
        Arrow a = e.launchProjectile(Arrow.class, v);
        a.setDamage(damage);
        a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        e.getWorld().playSound(eye, Sound.ENTITY_SKELETON_SHOOT, 0.6f, spread > 0 ? 1.6f : 0.8f);
    }

    void steer(LivingEntity e, String role, Location base, Player tp) {
        if (!(e instanceof Mob m)) return;
        double d = e.getLocation().distance(base);
        if ("negotiator".equals(role)) { if (d > 6) hop(e, base, 0.25); return; }
        if ("mortar".equals(role) || "sniper".equals(role)) {
            double hold = "mortar".equals(role) ? 42 : 55;
            if (d > hold) hop(e, base, 0.3); else m.setAware(false);
            return;
        }
        if (tp != null) m.setTarget(tp);
        if (isFlyer(e)) {
            if (tp == null) e.setVelocity(base.toVector().subtract(e.getLocation().toVector()).normalize().multiply(0.3));
            return;
        }
        if (tp == null) hop(e, base, 0.3);
    }

    /** Spigot не умеет строить путь, поэтому идём к цели прыжками. */
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
        if (horiz < 28 && c % 12 == 0) {
            Entity ent = e.getWorld().spawnEntity(pos.clone().add(0, -1, 0), EntityType.TNT);
            if (ent instanceof TNTPrimed t) {
                t.setFuseTicks(40);
                t.setVelocity(new Vector(v.getX() * 0.3, -0.1, v.getZ() * 0.3));
                e.getWorld().playSound(pos, Sound.ENTITY_TNT_PRIMED, 1f, 1f);
            }
        }
    }

    // ===== Пчёлы и дроны-смертники =====
    void tickBees() {
        for (Iterator<Map.Entry<UUID, Integer>> it = bees.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Integer> en = it.next();
            Entity ent = Bukkit.getEntity(en.getKey());
            if (ent == null) { it.remove(); continue; }
            if (!(ent instanceof Mob bee) || bee.isDead()) { it.remove(); continue; }
            String bk = baseOf(bee);
            Base b = bk == null ? null : bases.get(bk);
            if (b == null || b.w() == null) { boom(bee); it.remove(); continue; }
            int age = en.getValue() + 1;
            en.setValue(age);
            Location l = bee.getLocation();
            Player p = nearest(l, 90);
            Location target = p != null ? p.getEyeLocation() : b.loc().add(0, 1, 0);
            Vector vel = target.toVector().subtract(l.toVector());
            if (vel.lengthSquared() < 0.01) { boom(bee); it.remove(); continue; }
            vel.normalize().multiply(bee instanceof Vex ? 0.5 : 0.42);
            bee.setVelocity(vel);
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
        float power = bee instanceof Vex ? 4.5f : 3.5f;
        for (Entity p : new ArrayList<>(bee.getPassengers())) p.remove();
        bee.remove();
        l.getWorld().createExplosion(l, power, false, true);
    }

    // ===== ПВО: зенитка =====
    int upgradeCost(int level) { return 8 + 8 * level; }

    boolean isDirt(Material m) {
        return m == Material.DIRT || m == Material.GRASS_BLOCK || m == Material.COARSE_DIRT
                || m == Material.PODZOL || m == Material.ROOTED_DIRT || m == Material.MYCELIUM;
    }

    void turret(Base b) {
        World w = b.w();
        if (w == null || !w.isChunkLoaded(b.x >> 4, b.z >> 4)) return;
        int interval = Math.max(4, 25 - b.level);
        if (c % interval != 0) return;
        double range = 24 + b.level * 2.5;
        Location top = new Location(w, b.x + 0.5, b.y + 1.9 + 0.05 * b.level, b.z + 0.5);
        Entity best = null; double bd = range * range;
        for (UUID id : mobs) {
            Entity en = Bukkit.getEntity(id);
            if (en == null || en.isDead() || !en.getWorld().equals(w) || !air(en)) continue;
            double d = en.getLocation().distanceSquared(top);
            if (d < bd) { bd = d; best = en; }
        }
        for (UUID id : bees.keySet()) {
            Entity en = Bukkit.getEntity(id);
            if (en == null || en.isDead() || !en.getWorld().equals(w)) continue;
            double d = en.getLocation().distanceSquared(top);
            if (d < bd) { bd = d; best = en; }
        }
        if (best == null) return;
        int shots = 1 + b.level / 7;
        for (int i = 0; i < shots; i++) {
            Location mz = top.clone().add((rnd.nextDouble() - 0.5) * 0.4, 0, (rnd.nextDouble() - 0.5) * 0.4);
            Location tl = best.getLocation().add(0, best.getHeight() / 2, 0).add(best.getVelocity().multiply(Math.sqrt(bd) / 1.5));
            Vector dir = tl.toVector().subtract(mz.toVector());
            if (dir.lengthSquared() == 0) continue;
            dir.normalize();
            Fireball fb = w.spawn(mz, Fireball.class);
            fb.setDirection(dir);
            fb.setVelocity(dir.clone().multiply(1.6));
            fb.setYield(0.6f + 0.15f * b.level);
            fb.setIsIncendiary(false);
            fb.getPersistentDataContainer().set(AA, PersistentDataType.BYTE, (byte) 1);
        }
        w.spawnParticle(Particle.FLAME, top, 10, 0.15, 0.1, 0.15, 0.02);
        w.playSound(top, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.8f + b.level * 0.03f);
    }

    int barrels(int level) { return Math.min(10, 1 + (level - 1) / 2 + (level >= 20 ? 1 : 0)); }

    void part(Base b, Location o, Material m, float tx, float ty, float tz,
              float sx, float sy, float sz, Quaternionf rot, boolean glow) {
        BlockDisplay d = (BlockDisplay) o.getWorld().spawnEntity(o, EntityType.BLOCK_DISPLAY);
        d.setBlock(m.createBlockData());
        d.setTransformation(new Transformation(new Vector3f(tx, ty, tz),
                rot == null ? new Quaternionf() : rot, new Vector3f(sx, sy, sz), new Quaternionf()));
        d.setPersistent(true);
        d.getPersistentDataContainer().set(MODEL, PersistentDataType.STRING, b.key());
        if (glow) d.setGlowing(true);
    }

    void clearModel(Base b) {
        World w = b.w();
        if (w == null || !w.isChunkLoaded(b.x >> 4, b.z >> 4)) return;
        Location o = new Location(w, b.x + 0.5, b.y + 1, b.z + 0.5);
        for (Entity en : w.getNearbyEntities(o, 4, 10, 4)) {
            if (en instanceof BlockDisplay
                    && b.key().equals(en.getPersistentDataContainer().get(MODEL, PersistentDataType.STRING))) en.remove();
        }
    }

    /** У каждого из 20 уровней свой облик: материалы, число стволов, размеры, пилоны, радар, крылья, корона. */
    void buildModel(Base b) {
        World w = b.w();
        if (w == null) return;
        clearModel(b);
        int L = b.level, i = L - 1;
        Location o = new Location(w, b.x + 0.5, b.y + 1, b.z + 0.5);

        part(b, o, AA_BODY[i], -0.5f, 0f, -0.5f, 1f, 0.2f, 1f, null, false);
        float ch = 0.3f + 0.02f * L, cw = 0.45f + 0.01f * L;
        part(b, o, AA_BODY[i], -cw / 2, 0.2f, -cw / 2, cw, ch, cw, null, false);
        float hy = 0.2f + ch, hw = 0.6f + 0.015f * L;
        part(b, o, AA_HEAD[i], -hw / 2, hy, -hw / 2, hw, 0.3f, hw, null, L >= 15);

        int n = barrels(L);
        float r = Math.min(0.3f, 0.1f + 0.03f * n), len = 0.6f + 0.05f * L, bw = 0.1f;
        for (int k = 0; k < n; k++) {
            double a = 2 * Math.PI * k / n;
            float cx = (float) Math.cos(a) * r, cz = (float) Math.sin(a) * r;
            Quaternionf q = new Quaternionf().rotateAxis((float) Math.toRadians(10 + L / 2.0),
                    (float) Math.sin(a), 0f, (float) -Math.cos(a));
            part(b, o, AA_BARREL[i], cx - bw / 2, hy + 0.3f, cz - bw / 2, bw, len, bw, q, false);
        }
        if (L >= 5) {
            for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2)
                part(b, o, AA_BODY[i], sx * 0.42f - 0.06f, 0.2f, sz * 0.42f - 0.06f, 0.12f, 0.35f, 0.12f, null, false);
        }
        if (L >= 10) {
            part(b, o, Material.LIGHTNING_ROD, 0.3f, 0.2f, -0.45f, 0.1f, 0.9f, 0.1f, null, false);
            part(b, o, Material.TARGET, 0.1f, 1.05f, -0.65f, 0.5f, 0.1f, 0.5f, null, L >= 15);
        }
        if (L >= 15) {
            part(b, o, AA_BODY[i], -1.0f, 0.2f, -0.15f, 0.5f, 0.12f, 0.3f, null, false);
            part(b, o, AA_BODY[i], 0.5f, 0.2f, -0.15f, 0.5f, 0.12f, 0.3f, null, false);
        }
        if (L >= 20) {
            part(b, o, Material.BEACON, -0.2f, hy + 0.31f, -0.2f, 0.4f, 0.4f, 0.4f, null, true);
        }
        b.modelLevel = L;
    }

    // ===== События =====
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (!isCore(e.getItemInHand())) return;
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
        Bukkit.getScheduler().runTask(this, () -> buildModel(b));
        e.getPlayer().sendMessage(ChatColor.DARK_PURPLE + "Ядро установлено. Это зенитка 1 уровня: ПКМ землёй по ядру улучшает её. Скоро к вам придут гости...");
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Base b = bases.get(keyOf(e.getBlock()));
        if (b == null) return;
        dropBase(b);
        save();
        e.setDropItems(false);
        e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation().add(0.5, 0.5, 0.5), core(1));
        e.getPlayer().sendMessage(ChatColor.GRAY + "Ядро убрано. Мобы отступили.");
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block bl = e.getClickedBlock();
        if (bl == null) return;
        Base b = bases.get(keyOf(bl));
        if (b == null) return;
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (isCore(hand)) return;
        if (isDirt(hand.getType())) {
            e.setCancelled(true);
            if (b.level >= 20) { p.sendMessage(ChatColor.GOLD + "Зенитка уже максимального уровня: " + AA_NAMES[19]); return; }
            int cost = upgradeCost(b.level);
            int take = Math.min(cost - b.dirt, hand.getAmount());
            hand.setAmount(hand.getAmount() - take);
            b.dirt += take;
            if (b.dirt >= cost) {
                b.dirt -= cost;
                b.level++;
                if (b.level >= 20) b.dirt = 0;
                buildModel(b);
                Location fx = b.loc().add(0, 2, 0);
                b.w().spawnParticle(Particle.HAPPY_VILLAGER, fx, 40, 0.6, 0.8, 0.6, 0.1);
                b.w().playSound(fx, Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
                say(p.getName() + " улучшил зенитку до уровня " + b.level + ": " + AA_NAMES[b.level - 1] + "!");
            } else {
                p.sendMessage(ChatColor.GREEN + "Земля: " + b.dirt + "/" + cost + " до уровня " + (b.level + 1));
            }
            save();
            return;
        }
        if (hand.getType() == Material.AIR) {
            e.setCancelled(true);
            double range = 24 + b.level * 2.5;
            p.sendMessage(ChatColor.AQUA + "Зенитка: уровень " + b.level + "/20 «" + AA_NAMES[b.level - 1] + "». Дальность "
                    + (int) range + " бл., залп: " + (1 + b.level / 7) + " фаербол(а). "
                    + (b.level < 20 ? "Земля: " + b.dirt + "/" + upgradeCost(b.level) + "." : "Максимальный уровень!"));
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e) {
        for (Block bl : e.getBlocks()) if (isBase(bl)) { e.setCancelled(true); return; }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent e) {
        for (Block bl : e.getBlocks()) if (isBase(bl)) { e.setCancelled(true); return; }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e) {
        if (e.getEntity().getPersistentDataContainer().has(AA, PersistentDataType.BYTE)) e.blockList().clear();
        else e.blockList().removeIf(this::isBase);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e) { e.blockList().removeIf(this::isBase); }

    /** Огонь зенитки бьёт только врагов. */
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager().getPersistentDataContainer().has(AA, PersistentDataType.BYTE)) {
            String r = role(e.getEntity());
            if (r == null || r.equals("negotiator")) e.setCancelled(true);
        }
    }

    /** Снежки и палки жителей. */
    @EventHandler
    public void onHit(ProjectileHitEvent e) {
        Integer dmg = e.getEntity().getPersistentDataContainer().get(THROW, PersistentDataType.INTEGER);
        if (dmg == null) return;
        if (e.getHitEntity() instanceof LivingEntity le) {
            String r = role(le);
            if (r != null && !r.equals("negotiator")) {
                Object sh = e.getEntity().getShooter();
                if (sh instanceof Entity src) le.damage(dmg, src); else le.damage(dmg);
            }
        }
    }

    @EventHandler
    public void onLoot(LootGenerateEvent e) {
        if (e.getLootTable().getKey().getKey().startsWith("chests/") && rnd.nextInt(100) < ci("chest-chance"))
            e.getLoot().add(core(1));
    }

    @EventHandler
    public void onLoad(EntitiesLoadEvent e) {
        for (Entity en : e.getEntities()) {
            rescan(en);
            if (en instanceof BlockDisplay) {
                String k = en.getPersistentDataContainer().get(MODEL, PersistentDataType.STRING);
                if (k != null && !bases.containsKey(k)) en.remove();
            }
        }
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent e) {
        if ("negotiator".equals(role(e.getEntity()))) e.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Entity en = e.getRightClicked();
        if (!"negotiator".equals(role(en))) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
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
        String r = role(e.getEntity());
        if (r == null) return;
        if (r.equals("kamikaze")) {
            for (Entity p : new ArrayList<>(e.getEntity().getPassengers())) p.remove();
            bees.remove(e.getEntity().getUniqueId());
            e.getDrops().clear();
            return;
        }
        if (r.equals("negotiator")) {
            Base b = bases.get(baseOf(e.getEntity()));
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
            s.sendMessage("/raider give [игрок] [кол-во] | start | stop | wave [номер] | status | clear | level <1-20> | set <ключ> <значение> | config | reload");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "give" -> {
                Player t = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : (s instanceof Player p ? p : null);
                if (t == null) { s.sendMessage("Игрок не найден."); return true; }
                int n = 1;
                try { if (a.length > 2) n = Math.max(1, Math.min(64, Integer.parseInt(a[2]))); } catch (NumberFormatException ignored) { }
                t.getInventory().addItem(core(n));
                s.sendMessage("Выдано Ядро x" + n + " игроку " + t.getName());
            }
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
                Base b = nearestBase(s);
                if (b == null || a.length < 2) { s.sendMessage("Нужна база рядом и номер уровня 1-20."); return true; }
                try { b.level = Math.max(1, Math.min(20, Integer.parseInt(a[1]))); b.dirt = 0; buildModel(b); save();
                    s.sendMessage("Уровень зенитки: " + b.level + " «" + AA_NAMES[b.level - 1] + "»"); }
                catch (NumberFormatException ex) { s.sendMessage("Это не число."); }
            }
            case "status" -> {
                if (bases.isEmpty()) s.sendMessage("Баз нет.");
                for (Base b : bases.values())
                    s.sendMessage(b.key() + " | " + b.state + " | волна " + b.wave + " | таймер " + b.countdown
                            + " | врагов " + alive(b.key()) + " | зенитка ур. " + b.level);
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
                for (String k : getConfig().getKeys(true))
                    if (!getConfig().isConfigurationSection(k)) s.sendMessage(k + " = " + getConfig().get(k));
            }
            case "reload" -> { reloadConfig(); s.sendMessage("Конфиг перезагружен."); }
            default -> s.sendMessage("Неизвестная подкоманда.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] a) {
        if (a.length == 1) return List.of("give", "start", "stop", "wave", "status", "clear", "level", "set", "config", "reload");
        if (a.length == 2 && a[0].equalsIgnoreCase("set")) return new ArrayList<>(new TreeSet<>(ALIASES.keySet()));
        return Collections.emptyList();
    }
}
