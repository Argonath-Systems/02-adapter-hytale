/**
 * ECS Persistence Bridge - Hytale Component wrappers for Argonath POJOs.
 * 
 * <p>This package provides {@link com.hypixel.hytale.component.Component} 
 * implementations that wrap platform-agnostic Argonath data classes for automatic
 * persistence through Hytale's EntityStore system.
 * 
 * <h2>Architecture</h2>
 * <ul>
 *   <li>Business logic uses POJOs (e.g., {@code PlayerStatsData})</li>
 *   <li>This package wraps POJOs in ECS Components for persistence</li>
 *   <li>Components register via {@link com.argonathsystems.adapter.hytale.ecs.ArgonathComponentRegistry}</li>
 *   <li>Sync happens on player join/quit via {@link com.argonathsystems.adapter.hytale.ecs.ArgonathComponentSyncService}</li>
 * </ul>
 * 
 * <h2>Specification</h2>
 * <p>See SF-ARCHITECTURE-028-ecs-persistence-bridge.md for full documentation.
 * 
 * @author Argonath Systems Team
 * @since 4.0.0
 */
package com.argonathsystems.adapter.hytale.ecs;
