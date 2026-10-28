import React, { useState } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import {
  Smartphone,
  X,
  Usb,
  Copy,
  Check,
  ExternalLink,
  Terminal,
  ShieldCheck,
  Zap,
  Info,
  Download,
  FolderArchive,
  ArrowDownToLine,
  Layers,
  Sparkles,
} from 'lucide-react';
import { usePWAInstall } from '../../hooks/usePWAInstall';

interface RunOnMobileModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const RunOnMobileModal: React.FC<RunOnMobileModalProps> = ({ isOpen, onClose }) => {
  const [copiedUrl, setCopiedUrl] = useState(false);
  const [copiedCmd, setCopiedCmd] = useState(false);
  const [copiedAdb, setCopiedAdb] = useState(false);
  const [activeTab, setActiveTab] = useState<'apk' | 'instant' | 'usb' | 'studio'>('apk');
  const [usbDevice, setUsbDevice] = useState<string | null>(null);
  const [usbStatus, setUsbStatus] = useState<string>('');
  const [isConnectingUsb, setIsConnectingUsb] = useState(false);

  const { isInstallable, isInstalled, promptInstall } = usePWAInstall();

  if (!isOpen) return null;

  const currentUrl =
    window.location.origin.includes('localhost') || window.location.origin.includes('127.0.0.1')
      ? 'https://ais-pre-gaknqngtbhxi6rdmovuzim-954312821173.asia-southeast1.run.app'
      : window.location.href;

  const handleCopyUrl = () => {
    navigator.clipboard.writeText(currentUrl);
    setCopiedUrl(true);
    setTimeout(() => setCopiedUrl(false), 2000);
  };

  const adbCommand = `cd android-app && ./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n com.academialife.app/.MainActivity`;

  const handleCopyCmd = () => {
    navigator.clipboard.writeText(adbCommand);
    setCopiedCmd(true);
    setTimeout(() => setCopiedCmd(false), 2000);
  };

  const gradleAssembleCmd = `./gradlew assembleDebug`;
  const handleCopyAdb = () => {
    navigator.clipboard.writeText(gradleAssembleCmd);
    setCopiedAdb(true);
    setTimeout(() => setCopiedAdb(false), 2000);
  };

  const handleConnectWebUSB = async () => {
    if (!('usb' in navigator)) {
      setUsbStatus('WebUSB is not supported in this browser. Please use Google Chrome or Microsoft Edge.');
      return;
    }
    setIsConnectingUsb(true);
    setUsbStatus('Looking for USB devices... Select your Android phone in the browser popup.');
    try {
      const device = await (navigator as any).usb.requestDevice({
        filters: [
          { classCode: 255, subclassCode: 66, protocolCode: 1 },
          { classCode: 255 },
        ],
      });
      if (device) {
        setUsbDevice(
          `${device.productName || 'Android Device'} (${device.manufacturerName || 'Vendor ID: ' + device.vendorId})`
        );
        setUsbStatus('Device authorized & linked via WebUSB!');
      }
    } catch (err: any) {
      if (err.name === 'NotFoundError') {
        setUsbStatus('No device selected or connection cancelled.');
      } else {
        setUsbStatus(`USB Error: ${err.message || 'Could not connect'}`);
      }
    } finally {
      setIsConnectingUsb(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl shadow-2xl max-w-2xl w-full border border-slate-100 overflow-hidden flex flex-col max-h-[92vh]">
        {/* Header */}
        <div className="px-6 py-5 bg-gradient-to-r from-indigo-600 via-indigo-700 to-violet-700 text-white flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="p-2.5 bg-white/10 rounded-2xl backdrop-blur-md">
              <Smartphone className="w-6 h-6 text-white" />
            </div>
            <div>
              <h3 className="font-bold text-lg text-white">Download & Run on Mobile Phone</h3>
              <p className="text-xs text-indigo-100">Direct APK package, 1-Click Phone Install, and USB Deploy</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl text-white/80 hover:text-white hover:bg-white/10 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab Navigation */}
        <div className="flex border-b border-slate-200 bg-slate-50 px-4 sm:px-6 pt-3 space-x-2 sm:space-x-4 overflow-x-auto scrollbar-none">
          <button
            onClick={() => setActiveTab('apk')}
            className={`pb-3 text-xs font-bold transition-all whitespace-nowrap flex items-center space-x-1.5 ${
              activeTab === 'apk'
                ? 'text-indigo-600 border-b-2 border-indigo-600'
                : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            <Download className="w-4 h-4 text-emerald-600" />
            <span>Download APK Package</span>
          </button>

          <button
            onClick={() => setActiveTab('instant')}
            className={`pb-3 text-xs font-bold transition-all whitespace-nowrap flex items-center space-x-1.5 ${
              activeTab === 'instant'
                ? 'text-indigo-600 border-b-2 border-indigo-600'
                : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            <Zap className="w-4 h-4 text-amber-500" />
            <span>Direct Phone Install (WebAPK)</span>
          </button>

          <button
            onClick={() => setActiveTab('usb')}
            className={`pb-3 text-xs font-bold transition-all whitespace-nowrap flex items-center space-x-1.5 ${
              activeTab === 'usb'
                ? 'text-indigo-600 border-b-2 border-indigo-600'
                : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            <Usb className="w-4 h-4" />
            <span>USB Debug Deploy</span>
          </button>

          <button
            onClick={() => setActiveTab('studio')}
            className={`pb-3 text-xs font-bold transition-all whitespace-nowrap flex items-center space-x-1.5 ${
              activeTab === 'studio'
                ? 'text-indigo-600 border-b-2 border-indigo-600'
                : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            <Layers className="w-4 h-4" />
            <span>Android Studio Guide</span>
          </button>
        </div>

        {/* Tab Contents */}
        <div className="p-5 sm:p-6 overflow-y-auto space-y-5 text-slate-700">
          {/* TAB 1: APK Download */}
          {activeTab === 'apk' && (
            <div className="space-y-4">
              <div className="bg-emerald-50 border border-emerald-200 rounded-2xl p-4 flex items-start space-x-3">
                <div className="p-2 bg-emerald-600 text-white rounded-xl">
                  <ArrowDownToLine className="w-5 h-5" />
                </div>
                <div className="text-xs text-emerald-950 leading-relaxed">
                  <div className="font-bold text-sm text-emerald-900 mb-0.5">Android APK Project Package Ready</div>
                  Download the ready-to-build Android source package (.zip) with pre-configured Gradle build scripts and Kotlin activity. You can generate <code className="bg-emerald-100 px-1.5 py-0.5 rounded font-mono font-semibold">app-debug.apk</code> instantly!
                </div>
              </div>

              {/* Direct Download Button */}
              <div className="p-5 bg-gradient-to-br from-indigo-50/70 via-white to-violet-50/50 rounded-2xl border border-indigo-100 flex flex-col sm:flex-row items-center justify-between gap-4">
                <div className="flex items-center space-x-3.5">
                  <div className="p-3 bg-indigo-600 text-white rounded-2xl shadow-sm">
                    <FolderArchive className="w-6 h-6" />
                  </div>
                  <div>
                    <div className="text-sm font-bold text-slate-900">AcademiaLife-Android-Project.zip</div>
                    <div className="text-xs text-slate-500">Includes Kotlin Android app, Gradle wrapper & 1-click batch builder</div>
                  </div>
                </div>

                <a
                  href="/downloads/AcademiaLife-Android-Project.zip"
                  download="AcademiaLife-Android-Project.zip"
                  className="w-full sm:w-auto inline-flex items-center justify-center space-x-2 px-5 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow-md shadow-emerald-600/20 transition-all hover:scale-[1.02] active:scale-95"
                >
                  <Download className="w-4 h-4" />
                  <span>Download Project (.ZIP)</span>
                </a>
              </div>

              {/* 3 Step Instructions */}
              <div className="border border-slate-200 rounded-2xl p-4 bg-slate-50/60 space-y-3">
                <div className="text-xs font-bold text-slate-900 flex items-center space-x-1.5">
                  <Sparkles className="w-4 h-4 text-indigo-600" />
                  <span>How to get the .apk file onto your phone:</span>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div className="bg-white p-3 rounded-xl border border-slate-200 text-xs">
                    <div className="w-5 h-5 bg-indigo-100 text-indigo-700 font-bold rounded-full flex items-center justify-center text-[10px] mb-2">1</div>
                    <span className="font-bold text-slate-800">Unzip Download</span>
                    <p className="text-[11px] text-slate-500 mt-1">Extract <code className="text-indigo-600">AcademiaLife-Android-Project.zip</code> on your computer.</p>
                  </div>

                  <div className="bg-white p-3 rounded-xl border border-slate-200 text-xs">
                    <div className="w-5 h-5 bg-indigo-100 text-indigo-700 font-bold rounded-full flex items-center justify-center text-[10px] mb-2">2</div>
                    <span className="font-bold text-slate-800">Generate APK</span>
                    <p className="text-[11px] text-slate-500 mt-1">Double-click <code className="text-indigo-600">run_on_mobile.bat</code> (Windows) or run <code className="text-indigo-600">./run_on_mobile.sh</code>.</p>
                  </div>

                  <div className="bg-white p-3 rounded-xl border border-slate-200 text-xs">
                    <div className="w-5 h-5 bg-indigo-100 text-indigo-700 font-bold rounded-full flex items-center justify-center text-[10px] mb-2">3</div>
                    <span className="font-bold text-slate-800">Install APK</span>
                    <p className="text-[11px] text-slate-500 mt-1">The script automatically pushes & launches the APK on your connected USB phone!</p>
                  </div>
                </div>
              </div>

              {/* Exact output location info */}
              <div className="bg-slate-900 text-slate-300 rounded-2xl p-4 font-mono text-xs space-y-2">
                <div className="flex items-center justify-between text-slate-400 text-[11px] pb-1 border-b border-slate-800">
                  <span>Target APK File Location after build:</span>
                  <button
                    onClick={handleCopyAdb}
                    className="flex items-center space-x-1 text-slate-300 hover:text-white"
                  >
                    {copiedAdb ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    <span>{copiedAdb ? 'Copied' : 'Copy build command'}</span>
                  </button>
                </div>
                <p className="text-emerald-400 font-bold text-[11px]">
                  android-app/app/build/outputs/apk/debug/app-debug.apk
                </p>
                <p className="text-[10px] text-slate-500">
                  Command: <span className="text-slate-300">./gradlew assembleDebug</span>
                </p>
              </div>
            </div>
          )}

          {/* TAB 2: Instant PWA/WebAPK Install */}
          {activeTab === 'instant' && (
            <div className="space-y-4">
              <div className="bg-amber-50 border border-amber-200 rounded-2xl p-4 flex items-start space-x-3">
                <Zap className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
                <div className="text-xs text-amber-900 leading-relaxed">
                  <span className="font-bold">Fastest Mobile App Installation (Zero Setup Needed):</span> Android and iOS support direct WebAPK installation directly through the browser. It creates an app icon in your app launcher with full-screen experience and offline support!
                </div>
              </div>

              <div className="flex flex-col sm:flex-row items-center justify-center gap-6 py-2">
                <div className="p-4 bg-white rounded-2xl shadow-md border border-slate-200 flex flex-col items-center">
                  <QRCodeSVG
                    value={currentUrl}
                    size={170}
                    level="H"
                    includeMargin={true}
                  />
                  <span className="text-[10px] text-slate-400 font-medium mt-2">
                    Scan with Phone Camera
                  </span>
                </div>

                <div className="space-y-3 max-w-xs text-xs text-slate-600">
                  <div className="font-semibold text-slate-900 text-sm">
                    2 Steps to Install on Phone:
                  </div>
                  <ol className="list-decimal pl-4 space-y-1.5 text-xs text-slate-600">
                    <li>Scan the QR code with your mobile phone camera or open Chrome.</li>
                    <li>
                      Tap the <span className="font-bold text-indigo-700">"Install App"</span> banner or tap the 3 dots in Chrome and select <span className="font-bold text-slate-800">"Add to Home screen"</span>.
                    </li>
                    <li>Android will automatically build and install the native WebAPK package on your device!</li>
                  </ol>

                  {/* If user is already on a device supporting prompt */}
                  {isInstallable && (
                    <button
                      onClick={() => promptInstall()}
                      className="w-full py-2.5 px-4 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl font-bold flex items-center justify-center space-x-2 shadow-md shadow-indigo-600/20"
                    >
                      <Download className="w-4 h-4" />
                      <span>Install AcademiaLife Now</span>
                    </button>
                  )}

                  <div className="pt-2">
                    <button
                      onClick={handleCopyUrl}
                      className="w-full flex items-center justify-center space-x-1.5 px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-800 rounded-xl font-medium transition-colors"
                    >
                      {copiedUrl ? <Check className="w-4 h-4 text-emerald-600" /> : <Copy className="w-4 h-4" />}
                      <span>{copiedUrl ? 'URL Copied!' : 'Copy Direct App Link'}</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* TAB 3: USB ADB Deploy */}
          {activeTab === 'usb' && (
            <div className="space-y-4">
              <div className="bg-indigo-50 border border-indigo-200 rounded-2xl p-4 flex items-start space-x-3">
                <Usb className="w-5 h-5 text-indigo-600 shrink-0 mt-0.5" />
                <div className="text-xs text-indigo-900 leading-relaxed">
                  <span className="font-bold">USB Debugging & ADB Push:</span> Since your phone is connected with USB Debugging enabled, you can run the app directly using ADB command line or WebUSB link.
                </div>
              </div>

              <div className="bg-slate-900 rounded-2xl p-4 text-white space-y-3 font-mono text-xs">
                <div className="flex items-center justify-between text-slate-400 border-b border-slate-800 pb-2">
                  <span>1-Line Terminal Command:</span>
                  <button
                    onClick={handleCopyCmd}
                    className="flex items-center space-x-1 text-slate-300 hover:text-white transition-colors"
                  >
                    {copiedCmd ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    <span>{copiedCmd ? 'Copied' : 'Copy'}</span>
                  </button>
                </div>
                <div className="text-emerald-400 select-all overflow-x-auto whitespace-pre-wrap text-[11px]">
                  {adbCommand}
                </div>
              </div>

              <div className="border border-slate-200 rounded-2xl p-4 bg-slate-50 space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <div className={`w-3 h-3 rounded-full ${usbDevice ? 'bg-emerald-500 animate-pulse' : 'bg-amber-400'}`} />
                    <span className="text-xs font-bold text-slate-800">
                      {usbDevice ? 'WebUSB Connected' : 'WebUSB Browser Pairing'}
                    </span>
                  </div>
                  {usbDevice && (
                    <span className="text-[11px] font-semibold text-emerald-700 bg-emerald-100 px-2.5 py-0.5 rounded-full">
                      Linked
                    </span>
                  )}
                </div>

                {usbDevice ? (
                  <div className="p-3 bg-white rounded-xl border border-emerald-200 text-xs text-slate-800 font-medium">
                    📱 {usbDevice}
                  </div>
                ) : (
                  <p className="text-xs text-slate-500">
                    Pair Chrome directly with your connected phone via the WebUSB standard.
                  </p>
                )}

                <button
                  onClick={handleConnectWebUSB}
                  disabled={isConnectingUsb}
                  className="w-full py-2.5 px-4 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs rounded-xl shadow-sm transition-all flex items-center justify-center space-x-2 disabled:opacity-50"
                >
                  <Usb className="w-4 h-4" />
                  <span>{isConnectingUsb ? 'Searching USB Bus...' : usbDevice ? 'Re-detect Device' : 'Detect Connected Phone via WebUSB'}</span>
                </button>

                {usbStatus && (
                  <p className="text-[11px] text-slate-600 bg-white p-2.5 rounded-xl border border-slate-200">
                    {usbStatus}
                  </p>
                )}
              </div>
            </div>
          )}

          {/* TAB 4: Android Studio */}
          {activeTab === 'studio' && (
            <div className="space-y-4">
              <div className="bg-slate-50 border border-slate-200 rounded-2xl p-4 space-y-2 text-xs text-slate-700">
                <p className="font-bold text-slate-900 text-sm">Building the APK in Android Studio:</p>
                <ol className="list-decimal pl-4 space-y-2 text-xs text-slate-600">
                  <li>
                    Download the project ZIP from the <span className="font-bold text-indigo-600">"Download APK Package"</span> tab and extract it.
                  </li>
                  <li>
                    Open <strong>Android Studio</strong>, click <strong>File → Open...</strong> and choose the <code className="font-semibold text-slate-800">android-app</code> folder.
                  </li>
                  <li>
                    Wait for Gradle sync to complete (takes ~15 seconds).
                  </li>
                  <li>
                    To generate the APK file: Click top menu <strong>Build → Build Bundle(s) / APK(s) → Build APK(s)</strong>.
                  </li>
                  <li>
                    Android Studio will show a bottom right notification: <span className="font-bold text-emerald-700">"APK(s) generated successfully"</span>. Click <span className="font-bold underline">locate</span> to view <code className="bg-slate-200 px-1 rounded">app-debug.apk</code>!
                  </li>
                  <li>
                    To run directly onto your connected phone: Choose your phone in the device selector and click <strong>Run (▶)</strong>.
                  </li>
                </ol>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-4 bg-slate-50 border-t border-slate-200 flex items-center justify-between">
          <span className="text-[11px] text-slate-500">
            Firebase DB: <code className="text-indigo-600 font-mono">ai-studio-academialifestud</code>
          </span>
          <button
            onClick={onClose}
            className="px-4 py-2 bg-slate-200 hover:bg-slate-300 text-slate-800 rounded-xl text-xs font-bold transition-colors"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
