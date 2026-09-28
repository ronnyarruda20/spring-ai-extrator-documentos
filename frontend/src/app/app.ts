import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { Api } from './api';
import { Info, Resultado } from './modelos';
import { ResultadoPainel } from './resultado';

const NOMES: Record<string, string> = { 'google-genai': 'Gemini', anthropic: 'Claude' };

export const EXEMPLOS = [
  { arquivo: 'nota-fiscal-exemplo.pdf', rotulo: 'Nota fiscal' },
  { arquivo: 'boleto-exemplo.pdf', rotulo: 'Boleto' },
  { arquivo: 'boleto-adulterado.pdf', rotulo: 'Boleto adulterado' },
];

@Component({
  selector: 'app-root',
  imports: [ResultadoPainel],
  template: `
    <header>
      <h1>Extrator de documentos <span>nota fiscal e boleto</span></h1>
      @if (info(); as i) {
        <span class="modelo">{{ nomeProvedor(i) }}</span>
      }
    </header>

    <main>
      <section class="entrada">
        <label
          class="soltar"
          [class.ativo]="arrastando()"
          (dragover)="$event.preventDefault(); arrastando.set(true)"
          (dragleave)="arrastando.set(false)"
          (drop)="soltar($event)"
        >
          <input type="file" accept=".pdf,.png,.jpg,.jpeg" (change)="escolher($event)" />
          <strong>Envie uma nota fiscal ou um boleto</strong>
          <span>PDF, PNG ou JPG, até 10 MB. O documento vai para o modelo de IA e não é guardado.</span>
        </label>
        <div class="exemplos">
          <span>Ou teste com um exemplo fictício:</span>
          @for (e of exemplos; track e.arquivo) {
            <button type="button" (click)="usarExemplo(e.arquivo)" [disabled]="processando()">{{ e.rotulo }}</button>
          }
        </div>

        @if (previa(); as p) {
          <div class="previa">
            @if (ehImagem()) {
              <img [src]="p" alt="Prévia do documento enviado" />
            } @else {
              <iframe [src]="p" title="Prévia do documento enviado"></iframe>
            }
          </div>
        }
      </section>

      <section class="saida" aria-live="polite">
        @if (processando()) {
          <div class="carregando">
            <span class="spinner" aria-hidden="true"></span>
            Lendo {{ nomeArquivo() }} e conferindo os dados…
          </div>
        } @else if (erro(); as e) {
          <p class="erro" role="alert">{{ e }}</p>
        } @else if (resultado(); as r) {
          <app-resultado [resultado]="r" />
        } @else {
          <div class="vazio">
            <h2>Como funciona</h2>
            <ol>
              <li>O modelo de IA lê o documento direto, sem OCR, e devolve os campos em JSON.</li>
              <li>Regras no código conferem o que ele leu: dígitos da chave, do CNPJ e da linha digitável, somas e totais.</li>
              <li>O CNPJ é consultado na Receita pelo servidor MCP Brasil.</li>
              <li>No boleto, banco, valor e vencimento embutidos na linha digitável são comparados com o que está impresso.</li>
            </ol>
          </div>
        }
      </section>
    </main>
  `,
})
export class App implements OnInit, OnDestroy {
  private readonly api = inject(Api);
  private readonly sanitizer = inject(DomSanitizer);

  protected readonly exemplos = EXEMPLOS;
  protected readonly info = signal<Info | null>(null);
  protected readonly arrastando = signal(false);
  protected readonly processando = signal(false);
  protected readonly resultado = signal<Resultado | null>(null);
  protected readonly erro = signal<string | null>(null);
  protected readonly previa = signal<SafeResourceUrl | null>(null);
  protected readonly ehImagem = signal(false);
  protected readonly nomeArquivo = signal('');
  private urlPrevia: string | null = null;

  ngOnInit(): void {
    this.api
      .info()
      .then((i) => this.info.set(i))
      .catch(() => this.info.set(null));
  }

  ngOnDestroy(): void {
    this.liberarPrevia();
  }

  async processar(arquivo: File): Promise<void> {
    this.liberarPrevia();
    this.urlPrevia = URL.createObjectURL(arquivo);
    this.previa.set(this.sanitizer.bypassSecurityTrustResourceUrl(this.urlPrevia));
    this.ehImagem.set(arquivo.type.startsWith('image/'));
    this.nomeArquivo.set(arquivo.name);
    this.resultado.set(null);
    this.erro.set(null);
    this.processando.set(true);
    try {
      this.resultado.set(await this.api.extrair(arquivo));
    } catch (e) {
      this.erro.set((e as Error).message);
    } finally {
      this.processando.set(false);
    }
  }

  protected async usarExemplo(nome: string): Promise<void> {
    await this.processar(await this.api.exemplo(nome));
  }

  protected escolher(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const arquivo = input.files?.[0];
    input.value = '';
    if (arquivo) {
      this.processar(arquivo);
    }
  }

  protected soltar(evento: DragEvent): void {
    evento.preventDefault();
    this.arrastando.set(false);
    const arquivo = evento.dataTransfer?.files?.[0];
    if (arquivo) {
      this.processar(arquivo);
    }
  }

  protected nomeProvedor(i: Info): string {
    const nome = NOMES[i.provedor] ?? i.provedor;
    return i.modelo ? `${nome} · ${i.modelo}` : nome;
  }

  private liberarPrevia(): void {
    if (this.urlPrevia) {
      URL.revokeObjectURL(this.urlPrevia);
      this.urlPrevia = null;
    }
  }
}
