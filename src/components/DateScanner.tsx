import { useRef, useState, useCallback, useEffect } from 'react';
import { Loader2, X, Camera } from 'lucide-react';
import { supabase } from '@/integrations/supabase/client';
import { toast } from 'sonner';
import { parseExpirationDate, preprocessDateImage } from '@/lib/dateOcr';

interface DateScannerProps {
  onDateFound: (date: string) => void;
  onClose: () => void;
}

export function DateScanner({ onDateFound, onClose }: DateScannerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const targetRef = useRef<HTMLDivElement>(null);
  const [isScanning, setIsScanning] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const isScanningRef = useRef(false);
  const foundRef = useRef(false);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const lastErrorToastRef = useRef(0);
  const lastCloudFallbackRef = useRef(0);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: 'environment', width: { ideal: 1280 }, height: { ideal: 720 } },
        });
        if (cancelled) { stream.getTracks().forEach(t => t.stop()); return; }
        streamRef.current = stream;
        if (videoRef.current) videoRef.current.srcObject = stream;

        // Start auto-scanning after camera is ready
        intervalRef.current = setInterval(() => {
          if (!isScanningRef.current && !foundRef.current) {
            captureAndAnalyze(false);
          }
        }, 4500);
      } catch {
        if (!cancelled) setError("Impossible d'accéder à la caméra.");
      }
    })();
    return () => {
      cancelled = true;
      if (intervalRef.current) clearInterval(intervalRef.current);
      streamRef.current?.getTracks().forEach(t => t.stop());
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const showFriendlyScanError = useCallback((message: string, force = false) => {
    const now = Date.now();
    if (!force && now - lastErrorToastRef.current < 10000) return;
    lastErrorToastRef.current = now;
    toast.error(message, { duration: 3000 });
  }, []);

  const readFunctionError = useCallback(async (fnError: unknown, fallback?: string) => {
    const context = (fnError as { context?: Response })?.context;
    if (!context) return fallback;

    try {
      const body = await context.clone().json();
      return typeof body?.error === 'string' ? body.error : fallback;
    } catch {
      return fallback;
    }
  }, []);

  const captureTargetCanvas = useCallback(() => {
    const video = videoRef.current;
    const target = targetRef.current;
    if (!video || !target || video.videoWidth === 0 || video.videoHeight === 0) return null;

    const videoRect = video.getBoundingClientRect();
    const targetRect = target.getBoundingClientRect();
    const scale = Math.max(videoRect.width / video.videoWidth, videoRect.height / video.videoHeight);
    const renderedWidth = video.videoWidth * scale;
    const renderedHeight = video.videoHeight * scale;
    const offsetX = (videoRect.width - renderedWidth) / 2;
    const offsetY = (videoRect.height - renderedHeight) / 2;

    const sourceX = Math.max(0, Math.floor((targetRect.left - videoRect.left - offsetX) / scale));
    const sourceY = Math.max(0, Math.floor((targetRect.top - videoRect.top - offsetY) / scale));
    const sourceWidth = Math.min(video.videoWidth - sourceX, Math.ceil(targetRect.width / scale));
    const sourceHeight = Math.min(video.videoHeight - sourceY, Math.ceil(targetRect.height / scale));

    if (sourceWidth <= 0 || sourceHeight <= 0) return null;

    const canvas = canvasRef.current ?? document.createElement('canvas');
    canvas.width = sourceWidth;
    canvas.height = sourceHeight;
    const ctx = canvas.getContext('2d', { willReadFrequently: true });
    if (!ctx) return null;

    ctx.drawImage(video, sourceX, sourceY, sourceWidth, sourceHeight, 0, 0, sourceWidth, sourceHeight);
    return canvas;
  }, []);

  const runLocalOcr = useCallback(async (canvas: HTMLCanvasElement) => {
    const processedCanvas = preprocessDateImage(canvas);
    const Tesseract = await import('tesseract.js');
    const worker = await Tesseract.createWorker('eng');

    try {
      await worker.setParameters({
        tessedit_char_whitelist: '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzÀÂÄÇÉÈÊËÎÏÔÖÙÛÜàâäçéèêëîïôöùûü/- .',
        tessedit_pageseg_mode: Tesseract.PSM.SPARSE_TEXT,
        preserve_interword_spaces: '1',
      });
      const result = await worker.recognize(processedCanvas);
      return result.data.text;
    } finally {
      await worker.terminate();
    }
  }, []);

  const runCloudFallback = useCallback(async (canvas: HTMLCanvasElement) => {
    const now = Date.now();
    if (now - lastCloudFallbackRef.current < 30000) {
      throw new Error('fallback-cooldown');
    }
    lastCloudFallbackRef.current = now;

    const imageBase64 = canvas.toDataURL('image/jpeg', 0.72);
    const { data, error: fnError } = await supabase.functions.invoke('ocr-date', {
      body: { image: imageBase64 },
    });

    if (fnError || data?.error) {
      const rawMessage = data?.error || await readFunctionError(fnError, fnError?.message);
      throw new Error(rawMessage || 'cloud-fallback-failed');
    }

    return typeof data?.date === 'string' ? data.date : null;
  }, [readFunctionError]);

  const captureAndAnalyze = useCallback(async (showErrors = true) => {
    if (!videoRef.current || !canvasRef.current || isScanningRef.current || foundRef.current) return;
    isScanningRef.current = true;
    setIsScanning(true);

    try {
      const canvas = captureTargetCanvas();
      if (!canvas) {
        isScanningRef.current = false;
        setIsScanning(false);
        return;
      }

      const text = await runLocalOcr(canvas);
      const parsed = parseExpirationDate(text);
      let date = parsed?.date ?? null;

      if (!date && showErrors) {
        try {
          date = await runCloudFallback(canvas);
        } catch (fallbackError) {
          if ((fallbackError as Error).message === 'fallback-cooldown') {
            showFriendlyScanError('Réessayez dans quelques secondes ou saisissez la date manuellement.', true);
          }
        }
      }

      if (date) {
        foundRef.current = true;
        if (intervalRef.current) clearInterval(intervalRef.current);
        streamRef.current?.getTracks().forEach(t => t.stop());
        onDateFound(date);
      } else {
        if (showErrors) showFriendlyScanError('Date non lisible. Recadrez-la ou saisissez-la manuellement.', true);
        isScanningRef.current = false;
        setIsScanning(false);
      }
    } catch {
      if (showErrors) showFriendlyScanError('Analyse locale indisponible. Réessayez avec une meilleure photo.', true);
      isScanningRef.current = false;
      setIsScanning(false);
    }
  }, [captureTargetCanvas, onDateFound, runCloudFallback, runLocalOcr, showFriendlyScanError]);

  const handleClose = () => {
    if (intervalRef.current) clearInterval(intervalRef.current);
    streamRef.current?.getTracks().forEach(t => t.stop());
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 bg-background flex flex-col">
      <div className="flex items-center justify-between px-4 py-3 bg-primary text-primary-foreground">
        <div className="flex items-center gap-2">
          <Camera className="w-5 h-5" />
          <span className="font-bold text-base">Scanner la date</span>
        </div>
        <button
          onClick={handleClose}
          className="p-1.5 rounded-full bg-primary-foreground/20 hover:bg-primary-foreground/30 transition-colors"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      <div className="flex-1 relative bg-black">
        {error ? (
          <div className="flex items-center justify-center h-full">
            <p className="text-sm text-destructive">{error}</p>
          </div>
        ) : (
          <>
            <video ref={videoRef} autoPlay playsInline className="w-full h-full object-cover" />
            <canvas ref={canvasRef} className="hidden" />

            <div className="absolute inset-0 pointer-events-none flex items-center justify-center">
              <div ref={targetRef} className="w-72 h-40 relative">
                <div className="absolute top-0 left-0 w-8 h-8 border-primary rounded-tl-lg" style={{ borderWidth: '3px', borderRight: 'none', borderBottom: 'none' }} />
                <div className="absolute top-0 right-0 w-8 h-8 border-primary rounded-tr-lg" style={{ borderWidth: '3px', borderLeft: 'none', borderBottom: 'none' }} />
                <div className="absolute bottom-0 left-0 w-8 h-8 border-primary rounded-bl-lg" style={{ borderWidth: '3px', borderRight: 'none', borderTop: 'none' }} />
                <div className="absolute bottom-0 right-0 w-8 h-8 border-primary rounded-br-lg" style={{ borderWidth: '3px', borderLeft: 'none', borderTop: 'none' }} />
                <div className="absolute left-2 right-2 h-0.5 bg-primary/80 animate-pulse top-1/2" />
              </div>
            </div>

            {isScanning && (
              <div className="absolute inset-0 bg-black/40 flex items-center justify-center">
                <div className="bg-background/95 rounded-xl px-5 py-3 flex items-center gap-2">
                  <Loader2 className="w-5 h-5 animate-spin text-primary" />
                  <span className="text-sm font-bold">Lecture locale en cours...</span>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      <div className="px-4 py-5 bg-background flex flex-col items-center gap-3">
        <p className="text-sm text-muted-foreground text-center">
          {isScanning ? 'Analyse en cours…' : 'Cadrez la date de péremption, la détection est automatique'}
        </p>
        <button
          onClick={() => captureAndAnalyze(true)}
          disabled={isScanning}
          className="flex items-center gap-2 px-5 py-2.5 bg-primary text-primary-foreground rounded-xl font-bold text-sm disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <Camera className="w-4 h-4" />
          Capturer
        </button>
      </div>
    </div>
  );
}
