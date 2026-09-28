import { Component, computed, input, signal } from '@angular/core';

import * as f from './formato';
import { Resultado, Verificacao } from './modelos';

const TITULOS: Record<Resultado['situacao'], { titulo: string; texto: string }> = {
  CONFERIDO: { titulo: 'Tudo confere', texto: 'Os dados lidos passaram em todas as verificações.' },
  REVISAR: { titulo: 'Revise antes de usar', texto: 'Algumas verificações não puderam ser feitas.' },
  INCONSISTENTE: { titulo: 'Há inconsistências', texto: 'Algo no documento não bate. Veja os itens em vermelho.' },
};
const ORDEM: Record<Verificacao['status'], number> = { ERRO: 0, ALERTA: 1, OK: 2 };

@Component({
  selector: 'app-resultado',
  template: `
    @let r = resultado();
    <div class="situacao" [attr.data-situacao]="r.situacao" role="status">
      <strong>{{ titulo().titulo }}</strong>
      <span>{{ titulo().texto }}</span>
    </div>

    <h3>Verificações</h3>
    <ul class="verificacoes">
      @for (v of verificacoes(); track $index) {
        <li [attr.data-status]="v.status">
          <span class="icone" aria-hidden="true">{{ v.status === 'OK' ? '✓' : v.status === 'ALERTA' ? '!' : '✕' }}</span>
          <span class="sr">{{ v.status }}:</span>
          {{ v.mensagem }}
        </li>
      }
    </ul>

    @if (r.dados.notaFiscal; as n) {
      <h3>Nota fiscal nº {{ n.numero ?? '—' }} · série {{ n.serie ?? '—' }}</h3>
      <dl>
        <dt>Chave de acesso</dt><dd class="mono">{{ f.chaveNfe(n.chaveAcesso) }}</dd>
        <dt>Emissão</dt><dd>{{ f.data(n.dataEmissao) }}</dd>
        <dt>Emitente</dt><dd>{{ n.emitente?.nome ?? '—' }}<br /><span class="mono">{{ f.documento(n.emitente?.documento) }}</span></dd>
        <dt>Destinatário</dt><dd>{{ n.destinatario?.nome ?? '—' }}<br /><span class="mono">{{ f.documento(n.destinatario?.documento) }}</span></dd>
      </dl>
      <table>
        <thead><tr><th>Item</th><th>Qtd.</th><th>Unitário</th><th>Total</th></tr></thead>
        <tbody>
          @for (i of n.itens ?? []; track $index) {
            <tr><td>{{ i.descricao }}</td><td>{{ i.quantidade }}</td><td>{{ f.moeda(i.valorUnitario) }}</td><td>{{ f.moeda(i.valorTotal) }}</td></tr>
          }
        </tbody>
        <tfoot>
          <tr><td colspan="3">Produtos</td><td>{{ f.moeda(n.valorProdutos) }}</td></tr>
          <tr><td colspan="3">Frete</td><td>{{ f.moeda(n.valorFrete) }}</td></tr>
          <tr><td colspan="3">Desconto</td><td>{{ f.moeda(n.valorDesconto) }}</td></tr>
          <tr class="total"><td colspan="3">Total da nota</td><td>{{ f.moeda(n.valorTotal) }}</td></tr>
        </tfoot>
      </table>
    }

    @if (r.dados.boleto; as b) {
      <h3>Boleto · {{ b.nomeBancoImpresso ?? 'banco ' + (b.bancoImpresso ?? '—') }}</h3>
      <dl>
        <dt>Linha digitável</dt><dd class="mono">{{ f.linhaDigitavel(b.linhaDigitavel) }}</dd>
        <dt>Valor</dt><dd>{{ f.moeda(b.valorDocumento) }}</dd>
        <dt>Vencimento</dt><dd>{{ f.data(b.vencimento) }}</dd>
        <dt>Beneficiário</dt><dd>{{ b.beneficiario?.nome ?? '—' }}<br /><span class="mono">{{ f.documento(b.beneficiario?.documento) }}</span></dd>
        <dt>Pagador</dt><dd>{{ b.pagador?.nome ?? '—' }}<br /><span class="mono">{{ f.documento(b.pagador?.documento) }}</span></dd>
        <dt>Nosso número</dt><dd>{{ b.nossoNumero ?? '—' }}</dd>
      </dl>
    }

    <div class="acoes">
      <button type="button" class="secundario" (click)="verJson.set(!verJson())">
        {{ verJson() ? 'Esconder JSON' : 'Ver JSON' }}
      </button>
      <button type="button" class="secundario" (click)="baixar()">Baixar JSON</button>
      <span class="tempo">Lido em {{ (r.duracaoMs / 1000).toFixed(1) }} s</span>
    </div>
    @if (verJson()) {
      <pre class="json">{{ json() }}</pre>
    }
  `,
})
export class ResultadoPainel {
  readonly resultado = input.required<Resultado>();
  protected readonly f = f;
  protected readonly verJson = signal(false);

  protected readonly titulo = computed(() => TITULOS[this.resultado().situacao]);
  protected readonly verificacoes = computed(() =>
    [...this.resultado().verificacoes].sort((a, b) => ORDEM[a.status] - ORDEM[b.status]),
  );
  protected readonly json = computed(() => JSON.stringify(this.resultado(), null, 2));

  protected baixar(): void {
    const url = URL.createObjectURL(new Blob([this.json()], { type: 'application/json' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = 'extracao.json';
    a.click();
    URL.revokeObjectURL(url);
  }
}
