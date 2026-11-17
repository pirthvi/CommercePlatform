import { StoreInterface } from '@commerce/store';
import { inject, Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'admin-content-image',
  standalone: true,
  templateUrl: './content-image-picker.component.html',
  styleUrl: './content-image-picker.component.css',
})
export class ContentImagePickerComponent {
  readonly ui = inject(StoreInterface);
  @Input({ required: true }) label!: string;
  @Input({ required: true }) preview!: string;
  @Input({ required: true }) limitMb!: number;
  @Output() readonly fileChange = new EventEmitter<File | null>();
  dragging = false;
  error = '';

  choose(input: HTMLInputElement): void {
    this.select(input.files?.[0]);
    input.value = '';
  }

  drop(event: DragEvent): void {
    event.preventDefault();
    this.dragging = false;
    this.select(event.dataTransfer?.files[0]);
  }

  private select(file?: File): void {
    if (!file) return;
    if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type) || !file.size || file.size > this.limitMb * 1024 * 1024) {
      this.error = this.ui.text('content-image-picker.fileLimit', { limit: this.limitMb });
      return;
    }
    this.error = '';
    this.fileChange.emit(file);
  }
}
