package me.nao.manager.mg;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import me.nao.main.mg.Minegame;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TurretManager implements Listener {
    private Minegame plugin;
    private static final Set<Location> torretas = ConcurrentHashMap.newKeySet();
    private static final Map<Location, Long> cooldown = new ConcurrentHashMap<>();
    private int maxPorMundo = 100;
    private static final int RADIO_PASIVA = 30; // bloques para torreta pasiva
    private static final int RADIO_BUSQUEDA = 55; // bloques para buscar player
    private static final int RADIO_NO_DISPARO = 20; // bloques mínimos si no vuela
    private static final long COOLDOWN_GROUND = 800; // 0.8s para player caminando
    private static final long COOLDOWN_AIR = 50;     // 0.05s para elytra/TNT = Phalanx

    public TurretManager(Minegame plugin) {
        this.plugin = plugin;
    }

    
    public int getCount() {
    	return torretas.size();
    }
    
    // LLAMA ESTO DESDE onEnable DESPUÉS DE QUE CARGUEN LOS MUNDOS
    public void cargarTorretas() {
        FileConfiguration cfg = plugin.getTurrets();
        List<String> list = cfg.getStringList("Torretas");
        for (String s : list) {
            String[] parts = s.split(",");
            if (parts.length!= 4) continue;
            World w = Bukkit.getWorld(parts[0]);
            if (w!= null) torretas.add(new Location(w,
                Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3])));
        }
    }

    
    public void startTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (Bukkit.getOnlinePlayers().isEmpty() || torretas.isEmpty()) return;

            for (Location loc : new ArrayList<>(torretas)) {
                World w = loc.getWorld();
                if (w == null || !w.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) continue;
                if (!esTorretaValida(loc)) {
                    torretas.remove(loc);
                    cooldown.remove(loc);
                    guardarTorretas();
                    continue;
                }

                Location eye = loc.clone().add(0.5, 1.5, 0.5);
                
                // 1. PRIORIDAD 1: TNT/Proyectil → Phalanx 50ms
                boolean hayProyectil = !w.getNearbyEntities(eye, RADIO_BUSQUEDA, RADIO_BUSQUEDA, RADIO_BUSQUEDA, 
                    e -> e instanceof TNTPrimed || (e instanceof Projectile && !(e instanceof Arrow))
                ).isEmpty();
                
                if (hayProyectil) {
                    if (System.currentTimeMillis() - cooldown.getOrDefault(loc, 0L) >= COOLDOWN_AIR) {
                        buscarYDispararProyectiles(w, loc);
                        cooldown.put(loc, System.currentTimeMillis());
                        w.playSound(eye, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 2f);
                    }
                    continue; // IMPORTANTE: salta el resto
                }
                
                // 2. PRIORIDAD 2: Player volando → Phalanx 50ms
                Player targetVolando = buscarTarget(w, loc, true);
                if (targetVolando != null) {
                    if (System.currentTimeMillis() - cooldown.getOrDefault(loc, 0L) >= COOLDOWN_AIR) {
                        dispararRafaga(loc, targetVolando, true); // <- AGREGA EL TRUE
                        cooldown.put(loc, System.currentTimeMillis());
                        w.playSound(eye, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 2f);
                    }
                    continue;
                }
                
                // 3. PRIORIDAD 3: Player caminando → 800ms
                Player targetSuelo = buscarTarget(w, loc, false);
                if (targetSuelo != null) {
                    if (System.currentTimeMillis() - cooldown.getOrDefault(loc, 0L) >= COOLDOWN_GROUND) {
                        dispararRafaga(loc, targetSuelo, false); // <- AGREGA EL FALSE
                        cooldown.put(loc, System.currentTimeMillis());
                        w.playSound(eye, Sound.BLOCK_NOTE_BLOCK_PLING, 1.5f, 2.0f);
                    }
                    continue;
                }
                
                rotar(loc);
            }
        }, 0L, 1L);
    }
    
    
//    public void startTask() {
//        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
//            if (Bukkit.getOnlinePlayers().isEmpty() || torretas.isEmpty()) return;
//
//            for (Location loc : new ArrayList<>(torretas)) { // copia para evitar iterador viejo
//                World w = loc.getWorld();
//                if (w == null || !w.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) continue;
//                if (System.currentTimeMillis() - cooldown.getOrDefault(loc, 0L) < 50) continue;
//
//                // VALIDACIÓN: si la estructura se rompió, elimina la torreta
//                if (!esTorretaValida(loc)) {
//                    torretas.remove(loc);
//                    cooldown.remove(loc);
//                    guardarTorretas();
//                    
//                    continue;
//                }
//
//                Player target = buscarTarget(w, loc);
//                if (target != null) {
//                    dispararRafaga(loc, target);
//                    cooldown.put(loc, System.currentTimeMillis());
//                    w.playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.5f, 2.0f);
//                } else {
//                	buscarYDispararProyectiles(w, loc);
//                    rotar(loc);
//                }
//            }
//        }, 0L, 1L);
//    }

    private boolean esTorretaValida(Location loc) {
        Block b = loc.getBlock();
        if (b.getType() != Material.DISPENSER) return false;
        Block abajo1 = b.getRelative(BlockFace.DOWN);
        Block abajo2 = abajo1.getRelative(BlockFace.DOWN);
        return abajo1.getType() == Material.IRON_BLOCK && abajo2.getType() == Material.GOLD_BLOCK;
    }
    
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        Block b = e.getBlock();
        if (b.getType()!= Material.DISPENSER) return;

        World w = b.getWorld();
        long torretasEnMundo = torretas.stream().filter(l -> l.getWorld().equals(w)).count();
        if (torretasEnMundo >= maxPorMundo) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cLímite de torretas en este mundo alcanzado: " + maxPorMundo);
            return;
        }

        Block abajo1 = b.getRelative(BlockFace.DOWN);
        Block abajo2 = abajo1.getRelative(BlockFace.DOWN);
        if (abajo1.getType() == Material.IRON_BLOCK && abajo2.getType() == Material.GOLD_BLOCK) {
            torretas.add(b.getLocation());
            guardarTorretas();

            w.spawnParticle(Particle.END_ROD, b.getLocation().add(0.5, 1, 0.5), 15, 0.3, 0.3, 0.3, 0);
            w.playSound(b.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Location loc = e.getBlock().getLocation();
        if (torretas.remove(loc)) {
            cooldown.remove(loc);
            guardarTorretas();
            e.getPlayer().getWorld().playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 0.8f);
        }
    }

    // TORRETA PASIVA: dispara cuando alguien pone/rompe bloque cerca
    @EventHandler
    public void onPlacePasiva(BlockPlaceEvent e) {
        detectarYDisparar(e.getBlock().getLocation(), e.getPlayer());
    }

    @EventHandler
    public void onBreakPasiva(BlockBreakEvent e) {
        detectarYDisparar(e.getBlock().getLocation(), e.getPlayer());
    }

    
    @SuppressWarnings("deprecation")
    @EventHandler
    public void onArrowHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof Arrow)) return;
        Arrow arrow = (Arrow) e.getEntity();
        
        String name = arrow.getCustomName();
        if (name == null) return;
        
        // Solo procesa flechas de torreta
        if (!"ANTI_PROYECTIL".equals(name) && !name.contains("TORRETA")) return;
        
        // Remueve la flecha siempre para que no se quede pegada
        arrow.remove();
        
        if (e.getHitEntity() instanceof TNTPrimed) {
        	TNTPrimed tnt = (TNTPrimed) e.getHitEntity();
            Location loc = tnt.getLocation();
            World w = loc.getWorld();
            if (w == null) return;
            
            // 1. Partículas
            w.spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
            w.spawnParticle(Particle.SMOKE, loc, 15, 0.5, 0.5, 0.5, 0.05);
            w.spawnParticle(Particle.FLAME, loc, 20, 0.3, 0.3, 0.3, 0.02);
            
            // 2. Sonido
            w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.8f);
            
            // 3. Crits random
            for (int i = 0; i < 8; i++) {
                w.spawnParticle(Particle.CRIT, loc.clone().add(
                    (Math.random()-0.5)*0.5, 
                    (Math.random()-0.5)*0.5, 
                    (Math.random()-0.5)*0.5
                ), 3, 0, 0, 0, 0.1);
            }
            
            tnt.remove();
            w.createExplosion(loc, 0f, false, false);
        }
    }
    
    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e) {
        for (Block b : e.blockList()) {
            if (b.getType() == Material.DISPENSER) {
                Location loc = b.getLocation();
                if (torretas.contains(loc)) {
                    torretas.remove(loc);
                    cooldown.remove(loc);
                    guardarTorretas();
                    b.getWorld().dropItemNaturally(loc, new ItemStack(Material.DISPENSER));
                }
            }
        }
    }
    
    private boolean enRango(Location a, Location b, int bloques) {
        return a.getWorld().equals(b.getWorld()) 
            && a.distanceSquared(b) < bloques * bloques;
    }

    private boolean puedeSerHerido(Player p) {
        return (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) 
            && !p.isOp();
    }

    private void detectarYDisparar(Location blockLoc, Player p) {
        if (!puedeSerHerido(p)) return;
        
        World w = blockLoc.getWorld();
        if (w == null) return;
        
        torretas.stream()
          .filter(loc -> enRango(loc, blockLoc, RADIO_PASIVA))
          .forEach(torretaLoc -> {
                boolean esAire = p.isGliding(); // <- detecta si vuela
                dispararRafaga(torretaLoc, p, esAire);
                w.playSound(torretaLoc, Sound.BLOCK_NOTE_BLOCK_BASS, 2f, 0.5f);
            });
    }

    private Player buscarTarget(World w, Location torretaLoc, boolean soloElytra) {
        Location eye = torretaLoc.clone().add(0.5, 1.5, 0.5);
        
        return w.getNearbyEntities(eye, RADIO_BUSQUEDA, RADIO_BUSQUEDA, RADIO_BUSQUEDA, e -> e instanceof Player)
              .stream()
              .map(e -> (Player) e)
              .filter(p -> p.isOnline() && !p.isDead())
              .filter(this::puedeSerHerido)
              .filter(p -> {
                  // Si soloElytra=true: solo agarra los que vuelan
                  // Si soloElytra=false: solo agarra los que caminan DENTRO de RADIO_NO_DISPARO
                  if (soloElytra) {
                      return p.isGliding();
                  } else {
                      return !p.isGliding() && enRango(p.getLocation(), torretaLoc, RADIO_NO_DISPARO);
                  }
              })
              .filter(p -> tieneLineaDeVista(eye, p.getEyeLocation()))
              .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(eye)))
              .orElse(null);
    }
    
    private boolean tieneLineaDeVista(Location from, Location to) {
        return from.getWorld().rayTraceBlocks(from, to.toVector().subtract(from.toVector()),
                from.distance(to), FluidCollisionMode.NEVER, true) == null;
    }

    private Vector predecirElytra(Player p, Location torretaLoc) {
        if (!p.isGliding()) {
            Location loc = p.getLocation();
            Vector vel = p.getVelocity();
            double distancia = loc.distance(torretaLoc);
            double ticks = distancia / 3.5;
            
            // Si camina/salta, apunta al torso aprox Y=1.2
            double offsetY = 1.7; // base para que apunte al pecho
            
            if (vel.getY() > 0.1) offsetY += 0.8;      // saltando: súmale más
            else if (vel.getY() < -0.1) offsetY -= 0.3; // cayendo: bájale poco
            
            Vector prediccion = loc.clone().add(vel.multiply(ticks)).add(0, offsetY, 0).toVector();
            
            double gravedad = 0.08;
            prediccion.setY(prediccion.getY() - (gravedad * ticks * ticks * 0.5));
            return prediccion;
        }
        
        // Elytra sin cambios
        Vector vel = p.getVelocity().clone();
        Location loc = p.getLocation();
        double gravedad = 0.08;
        double distancia = loc.distance(torretaLoc);
        double ticks = distancia / 3.5;
        Vector prediccion = loc.clone().add(vel.multiply(ticks)).toVector();
        prediccion.setY(prediccion.getY() - (gravedad * ticks * ticks * 0.5));
        return prediccion;
    }

//    @SuppressWarnings("deprecation")
//	private void dispararRafaga(Location loc, Player target, boolean esAire) {
//        World w = loc.getWorld(); // <- AQUI estaba el error
//        if (w == null) return;
//        
//        Location eye = loc.clone().add(0.5, 1.5, 0.5);
//        Location targetEye = target.getEyeLocation();
//        
//        // Predicción para elytra
//        Vector velocity = target.getVelocity();
//        double ticksToImpact = eye.distance(targetEye) / (esAire ? 8.0 : 6.0); // 8f si es aire
//        
//        Location predictedPos = targetEye.clone().add(
//            velocity.getX() * ticksToImpact,
//            velocity.getY() * ticksToImpact,
//            velocity.getZ() * ticksToImpact
//        );
//        
//        Vector dir = predictedPos.subtract(eye).toVector().normalize();
//        
//        int flechas = esAire ? 1 : 3;
//        float velocidad = esAire ? 8f : 6f; // 8f para alcanzar cohetes
//        float spread = esAire ? 0.0f : 0.5f; // 0 spread para elytra
//        
//        for (int i = 0; i < flechas; i++) {
//            Arrow flecha = w.spawnArrow(eye, dir, velocidad, spread);
//            flecha.setDamage(esAire ? 8 : 4);
//            flecha.setCustomName("TORRETA:" + loc.getBlockX());
//        }
//    }
    
    
    @SuppressWarnings("deprecation")
	private void dispararRafaga(Location loc, Player target, boolean esAire) {
        World w = loc.getWorld();
        if (w == null) return;
        
        Location eye = loc.clone().add(0.5, 1.5, 0.5);
        
        // Usa tu predicción en vez de cálculo simple
        Vector targetPos = predecirElytra(target, loc);
        Vector dir = targetPos.subtract(eye.toVector()).normalize();
        
        int flechas = esAire ? 1 : 3;
        float velocidad = esAire ? 8f : 6f;
        float spread = esAire ? 0.0f : 0.5f;
        
        for (int i = 0; i < flechas; i++) {
            Arrow flecha = w.spawnArrow(eye, dir, velocidad, spread);
            flecha.setDamage(esAire ? 8 : 4);
            flecha.setCritical(true);
            flecha.setFireTicks(1200);
            flecha.setCustomName("TORRETA:" + loc.getBlockX());
        }
    }

    private void rotar(Location loc) {
        Location center = loc.clone().add(0.5, 1.5, 0.5);
        double angle = (System.currentTimeMillis() / 30.0) % 360 * Math.PI / 180;
        double x = Math.cos(angle) * 0.7;
        double z = Math.sin(angle) * 0.7;
        loc.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION,
            center.add(x, 0, z), 1, new Particle.DustTransition(Color.RED, Color.YELLOW, 1));
    }

    private void guardarTorretas() {
        FileConfiguration cfg = plugin.getTurrets();
        List<String> list = torretas.stream().map(l ->
            l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ()
        ).collect(Collectors.toList());
        cfg.set("Torretas", list);
        plugin.getTurrets().save();
        plugin.getTurrets().reload();
        
    }
    
    
//    @SuppressWarnings("deprecation")
//    private void buscarYDispararProyectiles(World w, Location torretaLoc) {
//        Location eye = torretaLoc.clone().add(0.5, 1.5, 0.5);
//        
//        w.getNearbyEntities(eye, RADIO_BUSQUEDA, RADIO_BUSQUEDA, RADIO_BUSQUEDA, e -> 
//            (e instanceof TNTPrimed) || (e instanceof Projectile && !(e instanceof Arrow))
//        ).forEach(proyectil -> {
//            if (System.currentTimeMillis() - cooldown.getOrDefault(torretaLoc, 0L) < 100) return;
//            
//            Location targetLoc = proyectil.getLocation();
//            Vector dir = targetLoc.subtract(eye).toVector().normalize();
//            
//            Arrow flecha = w.spawnArrow(eye, dir, 6f, 0.0f); // súbele a 6f para que alcance TNT
//            flecha.setDamage(10);
//            flecha.setCritical(true);
//            flecha.setCustomName("ANTI_PROYECTIL");
//            
//            cooldown.put(torretaLoc, System.currentTimeMillis());
//            w.playSound(eye, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 2f);
//        });
//    }
    
    @SuppressWarnings("deprecation")
	private void buscarYDispararProyectiles(World w, Location torretaLoc) {
        Location eye = torretaLoc.clone().add(0.5, 1.5, 0.5);
        
        w.getNearbyEntities(eye, RADIO_BUSQUEDA, RADIO_BUSQUEDA, RADIO_BUSQUEDA, e -> 
            (e instanceof TNTPrimed) || (e instanceof Projectile && !(e instanceof Arrow))
        ).forEach(proyectil -> {
            // QUITA ESTA LÍNEA: if (System.currentTimeMillis() - cooldown... < 100) return;
            
            Location targetLoc = proyectil.getLocation();
            Vector dir = targetLoc.subtract(eye).toVector().normalize();
            
            Arrow flecha = w.spawnArrow(eye, dir, 8f, 0.0f); // 8f para alcanzar TNT/cohetes
            flecha.setDamage(10);
            flecha.setCritical(true);
            flecha.setFireTicks(1200);
            flecha.setCustomName("ANTI_PROYECTIL");
        });
    }
    
}
