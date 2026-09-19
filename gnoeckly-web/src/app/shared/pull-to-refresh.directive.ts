import { Directive, ElementRef, inject, output } from '@angular/core';

/**
 * Android-typisches Pull-to-Refresh ohne Bibliothek: Touch-Ziehen nach unten am Seitenanfang
 * (> 80 px) loest {@code refresh} aus. Im Browser mit Maus passiert nichts.
 */
@Directive({
  selector: '[gnPullToRefresh]',
  host: {
    '(touchstart)': 'onStart($event)',
    '(touchmove)': 'onMove($event)',
    '(touchend)': 'onEnd()',
  },
})
export class PullToRefreshDirective {
  readonly refresh = output<void>();
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private startY: number | null = null;
  private pulled = 0;

  onStart(event: TouchEvent): void {
    this.startY = window.scrollY <= 0 ? event.touches[0].clientY : null;
    this.pulled = 0;
  }

  onMove(event: TouchEvent): void {
    if (this.startY === null) {
      return;
    }
    this.pulled = Math.max(0, event.touches[0].clientY - this.startY);
    this.host.nativeElement.style.transform = this.pulled > 0 ? `translateY(${Math.min(this.pulled / 2, 60)}px)` : '';
  }

  onEnd(): void {
    this.host.nativeElement.style.transform = '';
    if (this.startY !== null && this.pulled > 80) {
      this.refresh.emit();
    }
    this.startY = null;
    this.pulled = 0;
  }
}
