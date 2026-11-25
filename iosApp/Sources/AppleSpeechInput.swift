import AVFoundation
import ComixonEditor
import Speech

final class AppleSpeechInput: NSObject, IosSpeechInput {
    private let recognizer = SFSpeechRecognizer(locale: .current)
    private var engine: AVAudioEngine?
    private var request: SFSpeechAudioBufferRecognitionRequest?
    private var task: SFSpeechRecognitionTask?
    private var listener: IosSpeechListener?
    private var timeout: DispatchWorkItem?
    private var session = 0

    func start(listener: IosSpeechListener) {
        cancel()
        self.listener = listener
        let id = session
        SFSpeechRecognizer.requestAuthorization { [weak self] status in
            DispatchQueue.main.async {
                guard let self, self.session == id, self.listener != nil else { return }
                guard status == .authorized else { self.finish(error: "PERMISSION"); return }
                AVAudioSession.sharedInstance().requestRecordPermission { [weak self] allowed in
                    DispatchQueue.main.async {
                        guard let self, self.session == id, self.listener != nil else { return }
                        guard allowed else { self.finish(error: "PERMISSION"); return }
                        self.beginRecognition(session: id)
                    }
                }
            }
        }
    }

    func stop() {
        guard listener != nil else { return }
        // Releasing while a permission prompt is open cancels the pending recording.
        guard let request else { cancel(); return }
        engine?.stop()
        request.endAudio()
        scheduleTimeout(seconds: 5)
    }

    func cancel() {
        finish()
    }

    private func beginRecognition(session id: Int) {
        guard let recognizer else {
            finish(error: "LANGUAGE")
            return
        }
        guard recognizer.isAvailable else { finish(error: "UNAVAILABLE"); return }
        do {
            let audioSession = AVAudioSession.sharedInstance()
            try audioSession.setCategory(.record, mode: .measurement, options: .duckOthers)
            try audioSession.setActive(true, options: .notifyOthersOnDeactivation)
            let engine = AVAudioEngine()
            let request = SFSpeechAudioBufferRecognitionRequest()
            request.shouldReportPartialResults = true
            let input = engine.inputNode
            let format = input.outputFormat(forBus: 0)
            guard format.sampleRate > 0, format.channelCount > 0 else {
                finish(error: "UNAVAILABLE")
                return
            }
            self.engine = engine
            self.request = request
            input.installTap(onBus: 0, bufferSize: 1024, format: format) { buffer, _ in
                request.append(buffer)
            }
            task = recognizer.recognitionTask(with: request) { [weak self] result, error in
                DispatchQueue.main.async {
                    guard let self, self.session == id, self.listener != nil else { return }
                    if let result {
                        self.listener?.onText(text: result.bestTranscription.formattedString)
                        if result.isFinal { self.finish(); return }
                    }
                    if error != nil { self.finish(error: "FAILED") }
                }
            }
            engine.prepare()
            try engine.start()
            scheduleTimeout(seconds: 60)
        } catch {
            finish(error: "FAILED")
        }
    }

    private func scheduleTimeout(seconds: Double) {
        timeout?.cancel()
        let id = session
        let work = DispatchWorkItem { [weak self] in
            guard let self, self.session == id else { return }
            self.finish()
        }
        timeout = work
        DispatchQueue.main.asyncAfter(deadline: .now() + seconds, execute: work)
    }

    private func finish(error: String? = nil) {
        session += 1
        timeout?.cancel()
        timeout = nil
        engine?.stop()
        engine?.inputNode.removeTap(onBus: 0)
        engine = nil
        request?.endAudio()
        request = nil
        task?.cancel()
        task = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        let callback = listener
        listener = nil
        if let error { callback?.onError(code: error) }
        callback?.onFinished()
    }
}
