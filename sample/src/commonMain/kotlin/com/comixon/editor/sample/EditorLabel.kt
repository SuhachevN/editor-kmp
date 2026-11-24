package com.comixon.editor.sample

import androidx.compose.runtime.Composable
import com.comixon.editor.sample.resources.Res
import com.comixon.editor.sample.resources.add_frame
import com.comixon.editor.sample.resources.ai_filters
import com.comixon.editor.sample.resources.apply
import com.comixon.editor.sample.resources.authorization_required
import com.comixon.editor.sample.resources.back
import com.comixon.editor.sample.resources.basic
import com.comixon.editor.sample.resources.bridge
import com.comixon.editor.sample.resources.bubbles
import com.comixon.editor.sample.resources.cancel
import com.comixon.editor.sample.resources.choose_image
import com.comixon.editor.sample.resources.classic
import com.comixon.editor.sample.resources.classic_two
import com.comixon.editor.sample.resources.cool
import com.comixon.editor.sample.resources.corner
import com.comixon.editor.sample.resources.crop
import com.comixon.editor.sample.resources.delete
import com.comixon.editor.sample.resources.dictate
import com.comixon.editor.sample.resources.dictation_hint
import com.comixon.editor.sample.resources.done
import com.comixon.editor.sample.resources.export
import com.comixon.editor.sample.resources.export_strip
import com.comixon.editor.sample.resources.faded
import com.comixon.editor.sample.resources.filters
import com.comixon.editor.sample.resources.flip
import com.comixon.editor.sample.resources.font
import com.comixon.editor.sample.resources.frames
import com.comixon.editor.sample.resources.generate
import com.comixon.editor.sample.resources.import
import com.comixon.editor.sample.resources.instant
import com.comixon.editor.sample.resources.language
import com.comixon.editor.sample.resources.layers
import com.comixon.editor.sample.resources.lettering
import com.comixon.editor.sample.resources.mono
import com.comixon.editor.sample.resources.move_left
import com.comixon.editor.sample.resources.move_right
import com.comixon.editor.sample.resources.new_bubble
import com.comixon.editor.sample.resources.new_project
import com.comixon.editor.sample.resources.next
import com.comixon.editor.sample.resources.no_layers
import com.comixon.editor.sample.resources.old_film
import com.comixon.editor.sample.resources.old_tv
import com.comixon.editor.sample.resources.open
import com.comixon.editor.sample.resources.open_demo
import com.comixon.editor.sample.resources.original
import com.comixon.editor.sample.resources.pick_photos
import com.comixon.editor.sample.resources.pick_photos_hint
import com.comixon.editor.sample.resources.picture
import com.comixon.editor.sample.resources.previous
import com.comixon.editor.sample.resources.rectangles
import com.comixon.editor.sample.resources.redo
import com.comixon.editor.sample.resources.reset
import com.comixon.editor.sample.resources.rocks
import com.comixon.editor.sample.resources.rotate_left
import com.comixon.editor.sample.resources.rotate_right
import com.comixon.editor.sample.resources.save
import com.comixon.editor.sample.resources.screamo
import com.comixon.editor.sample.resources.server
import com.comixon.editor.sample.resources.settings
import com.comixon.editor.sample.resources.shape
import com.comixon.editor.sample.resources.size
import com.comixon.editor.sample.resources.speech_failed
import com.comixon.editor.sample.resources.speech_language
import com.comixon.editor.sample.resources.speech_network
import com.comixon.editor.sample.resources.speech_no_match
import com.comixon.editor.sample.resources.speech_permission
import com.comixon.editor.sample.resources.speech_unavailable
import com.comixon.editor.sample.resources.stairs
import com.comixon.editor.sample.resources.stop_dictation
import com.comixon.editor.sample.resources.tail
import com.comixon.editor.sample.resources.text
import com.comixon.editor.sample.resources.token
import com.comixon.editor.sample.resources.token_hint
import com.comixon.editor.sample.resources.undo
import com.comixon.editor.sample.resources.versions
import com.comixon.editor.sample.resources.warm
import com.comixon.editor.sample.resources.zoom
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal enum class EditorLabel(
    val resource: StringResource,
) {
    Dictate(Res.string.dictate),
    StopDictation(Res.string.stop_dictation),
    DictationHint(Res.string.dictation_hint),
    SpeechUnavailable(Res.string.speech_unavailable),
    SpeechPermission(Res.string.speech_permission),
    SpeechLanguage(Res.string.speech_language),
    SpeechNoMatch(Res.string.speech_no_match),
    SpeechNetwork(Res.string.speech_network),
    SpeechFailed(Res.string.speech_failed),
    AuthorizationRequired(Res.string.authorization_required),
    PickPhotos(Res.string.pick_photos),
    PickPhotosHint(Res.string.pick_photos_hint),
    OpenDemo(Res.string.open_demo),
    NewProject(Res.string.new_project),
    Frames(Res.string.frames),
    Crop(Res.string.crop),
    Bubbles(Res.string.bubbles),
    Lettering(Res.string.lettering),
    Filters(Res.string.filters),
    Back(Res.string.back),
    Done(Res.string.done),
    Cancel(Res.string.cancel),
    Undo(Res.string.undo),
    Redo(Res.string.redo),
    Open(Res.string.open),
    Save(Res.string.save),
    Import(Res.string.import),
    Export(Res.string.export),
    ExportStrip(Res.string.export_strip),
    Settings(Res.string.settings),
    Layers(Res.string.layers),
    Generate(Res.string.generate),
    Previous(Res.string.previous),
    Next(Res.string.next),
    AddFrame(Res.string.add_frame),
    Delete(Res.string.delete),
    MoveLeft(Res.string.move_left),
    MoveRight(Res.string.move_right),
    Text(Res.string.text),
    Font(Res.string.font),
    Size(Res.string.size),
    Tail(Res.string.tail),
    Picture(Res.string.picture),
    Shape(Res.string.shape),
    RotateLeft(Res.string.rotate_left),
    RotateRight(Res.string.rotate_right),
    Flip(Res.string.flip),
    Reset(Res.string.reset),
    Zoom(Res.string.zoom),
    Instant(Res.string.instant),
    AiFilters(Res.string.ai_filters),
    Versions(Res.string.versions),
    Server(Res.string.server),
    Token(Res.string.token),
    Language(Res.string.language),
    TokenHint(Res.string.token_hint),
    Apply(Res.string.apply),
    ChooseImage(Res.string.choose_image),
    NewBubble(Res.string.new_bubble),
    NoLayers(Res.string.no_layers),
    Classic(Res.string.classic),
    ClassicTwo(Res.string.classic_two),
    Rocks(Res.string.rocks),
    Rectangles(Res.string.rectangles),
    Screamo(Res.string.screamo),
    Corner(Res.string.corner),
    Basic(Res.string.basic),
    Stairs(Res.string.stairs),
    Bridge(Res.string.bridge),
    Original(Res.string.original),
    OldFilm(Res.string.old_film),
    OldTv(Res.string.old_tv),
    Warm(Res.string.warm),
    Cool(Res.string.cool),
    Mono(Res.string.mono),
    Faded(Res.string.faded),
}

@Composable
internal fun EditorLabel.text(): String = stringResource(resource)
