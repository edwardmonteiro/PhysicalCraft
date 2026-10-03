# PhysicalCraft 0.1.1

Correção de compatibilidade na abertura do mundo:
- Shaders OpenGL ES 2 usam a mesma precisão na passagem de dados entre etapas.
- Seleção de superfície gráfica aceita RGB e RGBA, com alternativa RGB565.
- Falhas de inicialização exibem diagnóstico copiável; falhas não tratadas são registradas somente no celular e mostradas na próxima abertura.
- Teste automatizado instala e executa o app em Android 12, verifica quadros renderizados, abre experimento e menu Gemma, salva e reabre antes de publicar.

Ainda é um protótipo. O emulador não substitui validação em cada GPU de celular. A inferência Gemma precisa ser testada separadamente com um modelo importado.
