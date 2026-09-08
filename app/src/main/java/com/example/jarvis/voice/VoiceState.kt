
package com.example.jarvis.voice

/**
 * JARVIS voice system ke current state ko represent karta hai.
 *
 * IDLE       = Voice system inactive
 * STANDBY    = Background me "Hey Jarvis" ka wait
 * LISTENING  = JARVIS user ki command sun raha hai
 * THINKING   = Command process / AI response ka wait
 * EXECUTING  = Android action execute ho raha hai
 * SPEAKING   = JARVIS response bol raha hai
 */
enum class VoiceState {

    IDLE,

    STANDBY,

    LISTENING,

    THINKING,

    EXECUTING,

    SPEAKING
}