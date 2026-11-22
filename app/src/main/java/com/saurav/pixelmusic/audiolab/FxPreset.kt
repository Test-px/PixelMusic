package com.saurav.pixelmusic.audiolab

import com.saurav.pixelmusic.audiolab.stage.StageSettings
import kotlinx.serialization.Serializable

/** A saved whole-chain Audio FX preset: every effect on the Audio FX screen captured under one name. */
@Serializable
data class FxPreset(
    val id: String,
    val name: String,
    val reverb: ReverbSettings = ReverbSettings(),
    val eightD: EightDSettings = EightDSettings(),
    val loudness: LoudnessSettings = LoudnessSettings(),
    val spatial: SpatialSettings = SpatialSettings(),
    val playback: PlaybackSettings = PlaybackSettings(),
    val volume: VolumeSettings = VolumeSettings(),
    // Saved without per-device speaker overrides so phone output settings stay intact when switching presets
    val stage: StageSettings = StageSettings(),
)
