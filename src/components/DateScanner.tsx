import { useRef, useState, useCallback, useEffect } from 'react';
import { Loader2, X, Camera } from 'lucide-react';
import { supabase } from '@/integrations/supabase/client';
import { toast } from 'sonner';

interface DateScannerProps {
  onDateFound: (date: string) => void;
  onClose: () => void;
}

export function DateScanner({ onDateFound, onClose }: DateScannerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [isScanning, setIsScanning] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const isScanningRef = useRef(false);
  const foundRef = useRef(false);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

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
            captureAndAnalyze();
          }
        }, 3000);
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

  const captureAndAnalyze = useCallback(async () => {
    if (!videoRef.current || !canvasRef.current || isScanningRef.current || foundRef.current) return;
    isScanningRef.current = true;
    setIsScanning(true);

    const canvas = canvasRef.current;
    const video = videoRef.current;
    if (video.videoWidth === 0 || video.videoHeight === 0) {
      isScanningRef.current = false;
      setIsScanning(false);
      return;
    }
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const ctx = canvas.getContext('2d');
    if (!ctx) { isScanningRef.current = false; setIsScanning(false); return; }
    ctx.drawImage(video, 0, 0);

    const imageBase64 = canvas.toDataURL('image/jpeg', 0.8);

    try {
      const { data, error: fnError } = await supabase.functions.invoke('ocr-date', {
        body: { image: imageBase64 },
      });

      if (fnError || data?.error) {
        const msg = data?.error || fnError?.message || 'Erreur OCR';
        toast.error(msg, { duration: 4000 });
        isScanningRef.current = false;
        setIsScanning(false);
        return;
      }

      if (data.date) {
        foundRef.current = true;
        if (intervalRef.current) clearInterval(intervalRef.current);
        streamRef.current?.getTracks().forEach(t => t.stop());
        onDateFound(data.date);
      } else {
        isScanningRef.current = false;
        setIsScanning(false);
      }
    } catch {
      isScanningRef.current = false;
      setIsScanning(false);
    }
  }, [onDateFound]);

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
              <div className="w-72 h-40 relative">
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
                  <span className="text-sm font-bold">Analyse IA en cours...</span>
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
          onClick={captureAndAnalyze}
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
