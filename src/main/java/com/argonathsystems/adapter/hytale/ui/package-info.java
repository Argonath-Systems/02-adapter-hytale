/**
 * HyUI adapter implementations for UI framework.
 * 
 * <p>This package contains the HyUI-specific implementations of the UI framework
 * interfaces. It bridges the platform-agnostic UI definitions with HyUI's
 * rendering capabilities.
 * 
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link com.argonathsystems.adapter.hytale.ui.HyUITemplateProcessor} - 
 *       HyUI implementation of TemplateProcessorWrapper</li>
 *   <li>{@link com.argonathsystems.adapter.hytale.ui.HyUITemplateProcessorFactory} - 
 *       Factory for creating HyUI template processors</li>
 * </ul>
 * 
 * <h2>Architectural Notes</h2>
 * <p>This is the ONLY package that should import from {@code au.ellie.hyui.*}.
 * All HyUI-specific code is isolated here to maintain platform portability
 * of the framework layer.
 * 
 * @author Argonath Systems Team
 * @version 1.1.0
 * @since 1.1.0
 */
package com.argonathsystems.adapter.hytale.ui;
