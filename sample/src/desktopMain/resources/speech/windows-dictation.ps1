param([string]$Language = 'en-US', [switch]$SelfTest)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Speech
Add-Type -ReferencedAssemblies System.Speech -TypeDefinition @'
using System;
using System.Globalization;
using System.Speech.Recognition;
using System.Text;
using System.Threading;

public static class ComixonDictation {
    static readonly object OutputLock = new object();
    static void Emit(string kind, string value = "") {
        lock (OutputLock) { Console.WriteLine(kind + "\t" + value); Console.Out.Flush(); }
    }
    static void Text(string text) { Emit("text", Convert.ToBase64String(Encoding.UTF8.GetBytes(text))); }
    public static void Run(string language) {
        Console.OutputEncoding = new UTF8Encoding(false);
        RecognizerInfo selected = null;
        foreach (var item in SpeechRecognitionEngine.InstalledRecognizers()) {
            if (item.Culture.Name.Equals(language, StringComparison.OrdinalIgnoreCase)) selected = item;
        }
        if (selected == null) { Emit("error", "LANGUAGE"); Emit("done"); return; }
        try {
            using (var engine = new SpeechRecognitionEngine(selected))
            using (var finished = new ManualResetEvent(false)) {
                engine.LoadGrammar(new DictationGrammar());
                engine.SetInputToDefaultAudioDevice();
                engine.SpeechHypothesized += (sender, e) => Text(e.Result.Text);
                engine.SpeechRecognized += (sender, e) => Text(e.Result.Text);
                engine.SpeechRecognitionRejected += (sender, e) => Emit("error", "NO_MATCH");
                engine.RecognizeCompleted += (sender, e) => { if (e.Error != null) Emit("error", "FAILED"); finished.Set(); };
                engine.RecognizeAsync(RecognizeMode.Single);
                var commands = new Thread(() => {
                    if (Console.ReadLine() == "stop") { try { engine.RecognizeAsyncStop(); } catch (InvalidOperationException) {} }
                    else { try { engine.RecognizeAsyncCancel(); } catch (InvalidOperationException) {} }
                });
                commands.IsBackground = true;
                commands.Start();
                if (!finished.WaitOne(TimeSpan.FromSeconds(65))) engine.RecognizeAsyncCancel();
            }
        } catch (UnauthorizedAccessException) { Emit("error", "PERMISSION"); }
          catch (Exception) { Emit("error", "FAILED"); }
        Emit("done");
    }
}
'@
if ($SelfTest) { [System.Speech.Recognition.SpeechRecognitionEngine]::InstalledRecognizers() | ForEach-Object { $_.Culture.Name } }
else { [ComixonDictation]::Run($Language) }
