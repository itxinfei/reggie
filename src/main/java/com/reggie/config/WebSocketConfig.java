package com.reggie.config;

import org.springframework.context.annotation.Configuration;

/**
 * WebSocket Configuration (Simplified - using polling instead)
 * 
 * @author reggie
 * @since 2026-08-11
 */
@Configuration
public class WebSocketConfig {
    // Using HTTP polling for real-time updates instead of WebSocket
    // This avoids WebSocket dependency issues
}
