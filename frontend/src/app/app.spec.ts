import { TestBed } from '@angular/core/testing';

import { Api } from './api';
import { App } from './app';
import { Resultado } from './modelos';

const ADULTERADO: Resultado = {
  tipo: 'BOLETO',
  situacao: 'INCONSISTENTE',
  duracaoMs: 4200,
  dados: {
    tipo: 'BOLETO',
    notaFiscal: null,
    observacoes: [],
    boleto: {
      linhaDigitavel: '23793381286007000000901234567897115950000189000',
      bancoImpresso: '001',
      nomeBancoImpresso: 'Banco do Brasil',
      beneficiario: { nome: 'Escola de Idiomas Exemplo Ltda', documento: '90126225000181' },
      pagador: { nome: 'Empresa Exemplo Ltda', documento: '90216739000128' },
      vencimento: '2026-10-10',
      valorDocumento: 189,
      nossoNumero: '00012345-6',
    },
  },
  verificacoes: [
    { campo: 'linhaDigitavel', status: 'OK', mensagem: 'Dígitos verificadores da linha digitável conferem.' },
    { campo: 'bancoImpresso', status: 'ERRO', mensagem: 'A linha digitável é do Bradesco (237), mas o cabeçalho diz Banco do Brasil (001).' },
    { campo: 'valorDocumento', status: 'ERRO', mensagem: 'A linha digitável cobra R$ 1.890,00, mas o valor impresso é R$ 189,00.' },
  ],
};

describe('App', () => {
  let enviados: string[];

  beforeEach(async () => {
    enviados = [];
    globalThis.URL.createObjectURL ??= () => 'blob:teste';
    globalThis.URL.revokeObjectURL ??= () => undefined;
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        {
          provide: Api,
          useValue: {
            info: async () => ({ provedor: 'google-genai', modelo: 'gemini-3.8-flash' }),
            exemplo: async (nome: string) => new File(['%PDF'], nome, { type: 'application/pdf' }),
            extrair: async (f: File) => {
              enviados.push(f.name);
              return ADULTERADO;
            },
          },
        },
      ],
    }).compileComponents();
  });

  async function renderizar() {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await new Promise((r) => setTimeout(r));
    await fixture.whenStable();
    return fixture;
  }

  it('mostra o provedor e os três exemplos', async () => {
    const el = (await renderizar()).nativeElement as HTMLElement;

    expect(el.querySelector('.modelo')?.textContent).toBe('Gemini · gemini-3.8-flash');
    expect([...el.querySelectorAll('.exemplos button')].map((b) => b.textContent)).toEqual([
      'Nota fiscal',
      'Boleto',
      'Boleto adulterado',
    ]);
  });

  it('boleto adulterado aparece como inconsistente, com os erros primeiro', async () => {
    const fixture = await renderizar();
    const el = fixture.nativeElement as HTMLElement;

    (el.querySelectorAll('.exemplos button')[2] as HTMLButtonElement).click();
    await new Promise((r) => setTimeout(r, 10));
    await fixture.whenStable();

    expect(enviados).toEqual(['boleto-adulterado.pdf']);
    expect(el.querySelector('.situacao')?.getAttribute('data-situacao')).toBe('INCONSISTENTE');
    const itens = [...el.querySelectorAll('.verificacoes li')].map((li) => li.getAttribute('data-status'));
    expect(itens).toEqual(['ERRO', 'ERRO', 'OK']);
    expect(el.querySelector('dd.mono')?.textContent).toBe('23793.38128 60070.000009 01234.567897 1 15950000189000');
  });
});
