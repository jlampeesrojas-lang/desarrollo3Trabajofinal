import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { Chatlistacomponent } from '../chatlistacomponent/chatlistacomponent';

@Component({
  selector: 'app-paginachatcomponent',
  standalone: true,
  imports: [RouterOutlet, Chatlistacomponent],
  templateUrl: './paginachatcomponent.html',
  styleUrl: './paginachatcomponent.css',
})
export class Paginachatcomponent {
  private readonly route = inject(ActivatedRoute);
  readonly hayChat = signal(this.route.snapshot.firstChild?.paramMap.get('id') != null);

  constructor() {
    inject(Router).events
      .pipe(filter(e => e instanceof NavigationEnd), takeUntilDestroyed())
      .subscribe(() => this.hayChat.set(this.route.snapshot.firstChild?.paramMap.get('id') != null));
  }
}
