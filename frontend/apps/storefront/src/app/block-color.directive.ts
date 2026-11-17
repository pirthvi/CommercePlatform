import { Directive, ElementRef, Input, OnChanges, Renderer2, RendererStyleFlags2, inject } from '@angular/core';
import { commerceColorPalette } from '@commerce/store';

const COLOR_PROPERTIES = [
  '--commerce-theme-color', '--commerce-theme-dark', '--commerce-theme-foreground',
  '--commerce-component-color', '--commerce-component-dark', '--commerce-component-soft', '--commerce-component-foreground',
  '--olive', '--olive-dark', '--honey',
];

@Directive({ selector: '[shopBlockColor]', standalone: true })
export class BlockColorDirective implements OnChanges {
  @Input() shopBlockColor?: string | null;
  private readonly element = inject(ElementRef<HTMLElement>);
  private readonly renderer = inject(Renderer2);

  ngOnChanges(): void {
    const host = this.element.nativeElement;
    for (const property of COLOR_PROPERTIES) this.renderer.removeStyle(host, property, RendererStyleFlags2.DashCase);
    if (!this.shopBlockColor || !/^#[0-9a-f]{6}$/i.test(this.shopBlockColor)) return;

    const palette = commerceColorPalette(this.shopBlockColor, '#536344');
    const overrides: Record<string, string> = {
      '--commerce-theme-color': palette.color,
      '--commerce-theme-dark': palette.dark,
      '--commerce-theme-foreground': palette.foreground,
      '--commerce-component-color': palette.color,
      '--commerce-component-dark': palette.dark,
      '--commerce-component-soft': palette.soft,
      '--commerce-component-foreground': palette.foreground,
      '--olive': palette.color,
      '--olive-dark': palette.dark,
      '--honey': palette.color,
    };
    for (const [property, value] of Object.entries(overrides)) {
      this.renderer.setStyle(host, property, value, RendererStyleFlags2.DashCase);
    }
  }
}
