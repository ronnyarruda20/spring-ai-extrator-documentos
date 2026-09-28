import { chaveNfe, data, documento, linhaDigitavel, moeda } from './formato';

describe('formato', () => {
  it('formata CNPJ, CNPJ alfanumérico e CPF', () => {
    expect(documento('90770487000184')).toBe('90.770.487/0001-84');
    expect(documento('12ABC34501DE35')).toBe('12.ABC.345/01DE-35');
    expect(documento('52998224725')).toBe('529.982.247-25');
    expect(documento(null)).toBe('—');
  });

  it('agrupa a chave de acesso de 4 em 4', () => {
    expect(chaveNfe('51260990770487000184550010000012341123456789')).toBe(
      '5126 0990 7704 8700 0184 5500 1000 0012 3411 2345 6789',
    );
  });

  it('formata a linha digitável como no boleto', () => {
    expect(linhaDigitavel('00190000090123456789701234567178315950000018900')).toBe(
      '00190.00009 01234.567897 01234.567178 3 15950000018900',
    );
  });

  it('formata moeda e data', () => {
    expect(moeda(1890)).toContain('1.890,00');
    expect(data('2026-10-10')).toBe('10/10/2026');
  });
});
