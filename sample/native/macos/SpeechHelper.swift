import AVFoundation
import Foundation
import Speech

private func emit(_ kind: String, _ value: String = "") {
    let line = kind + "\t" + value + "\n"
    FileHandle.standardOutput.write(Data(line.utf8))
}

private final class Dictation {
    private let engine = AVAudioEngine()
    private let recognizer: SFSpeechRecognizer?
    private let request = SFSpeechAudioBufferRecognitionRequest()
    private var task: SFSpeechRecognitionTask?
    private var tapInstalled = false
    private var finished = false
    private var stopping = false

    init(language: String) {
        recognizer = SFSpeechRecognizer(locale: Locale(identifier: language))
    }

    func start() {
        SFSpeechRecognizer.requestAuthorization { status in
            DispatchQueue.main.async {
                guard status == .authorized else { self.fail("PERMISSION"); return }
                AVCaptureDevice.requestAccess(for: .audio) { granted in
                    DispatchQueue.main.async {
                        guard granted else { self.fail("PERMISSION"); return }
                        self.listen()
                    }
                }
            }
        }
    }

    private func listen() {
        guard !finished else { return }
        if stopping { finish(); return }
        guard let recognizer = recognizer, recognizer.isAvailable else { fail("LANGUAGE"); return }
        request.shouldReportPartialResults = true
        request.taskHint = .dictation
        let input = engine.inputNode
        let format = input.outputFormat(forBus: 0)
        guard format.sampleRate > 0, format.channelCount > 0 else { fail("UNAVAILABLE"); return }
        input.installTap(onBus: 0, bufferSize: 1024, format: format) { buffer, _ in self.request.append(buffer) }
        tapInstalled = true
        task = recognizer.recognitionTask(with: request) { result, error in
            DispatchQueue.main.async {
                guard !self.finished else { return }
                if let result = result {
                    emit("text", Data(result.bestTranscription.formattedString.utf8).base64EncodedString())
                    if result.isFinal { self.finish(); return }
                }
                if error != nil { self.fail("FAILED") }
            }
        }
        do {
            engine.prepare()
            try engine.start()
            DispatchQueue.main.asyncAfter(deadline: .now() + 60) { self.stop() }
        } catch { fail("FAILED") }
    }

    func stop() {
        guard !finished, !stopping else { return }
        stopping = true
        stopAudio()
        request.endAudio()
        DispatchQueue.main.asyncAfter(deadline: .now() + 5) { self.finish() }
    }

    private func stopAudio() {
        engine.stop()
        if tapInstalled { engine.inputNode.removeTap(onBus: 0); tapInstalled = false }
    }

    func finish() {
        guard !finished else { return }
        finished = true
        stopAudio()
        task?.cancel()
        emit("done")
        exit(0)
    }

    private func fail(_ reason: String) {
        emit("error", reason)
        finish()
    }
}

if CommandLine.arguments.contains("--self-test") {
    guard !SFSpeechRecognizer.supportedLocales().isEmpty else { exit(1) }
    emit("done")
    exit(0)
}
private let dictation = Dictation(language: CommandLine.arguments.dropFirst().first ?? Locale.current.identifier)
DispatchQueue.global(qos: .userInitiated).async {
    let command = readLine()
    DispatchQueue.main.async {
        if command == "stop" { dictation.stop() } else { dictation.finish() }
    }
}
dictation.start()
RunLoop.main.run()
