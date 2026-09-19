import { Pipe, PipeTransform } from '@angular/core';

/** "vor 3 Min.", "vor 2 Std.", "vor 5 Tagen" - ohne Bibliothek, deutsch. */
@Pipe({ name: 'relativeTime' })
export class RelativeTimePipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) {
      return '';
    }
    const seconds = Math.max(0, Math.round((Date.now() - new Date(value).getTime()) / 1000));
    if (seconds < 60) {
      return 'gerade eben';
    }
    const minutes = Math.round(seconds / 60);
    if (minutes < 60) {
      return `vor ${minutes} Min.`;
    }
    const hours = Math.round(minutes / 60);
    if (hours < 24) {
      return `vor ${hours} Std.`;
    }
    const days = Math.round(hours / 24);
    if (days < 30) {
      return days === 1 ? 'gestern' : `vor ${days} Tagen`;
    }
    const months = Math.round(days / 30);
    return months < 12 ? `vor ${months} Mon.` : `vor ${Math.round(months / 12)} J.`;
  }
}

/** 12 -> "12 Gnöcken", 1 -> "1 Gnöcken" (Plural = Singular). */
@Pipe({ name: 'gnoecken' })
export class GnoeckenPipe implements PipeTransform {
  transform(value: number | null | undefined, signed = false): string {
    const amount = value ?? 0;
    const prefix = signed && amount > 0 ? '+' : '';
    return `${prefix}${amount.toLocaleString('de-AT')} Gnöcken`;
  }
}
