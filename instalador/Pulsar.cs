// Instalador e atualizador do Pulsar Client: uma janela própria, no visual do launcher, no lugar das telas do Windows.
//
// Instalar: o instalador NSIS gerado pelo electron-builder vai dentro deste .exe (recurso "setup.exe") e roda
// em modo silencioso; o progresso vem do tamanho da pasta instalada. Atualizar: o launcher baixa a versão nova,
// fecha e chama este programa com --atualizar <instalador baixado>; no fim o Pulsar abre de novo sozinho.
//
// Compilado no CI pelo csc do .NET Framework 4 que vem no Windows (C# 5), sem nada para instalar.
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.Drawing.Text;
using System.IO;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Threading;
using System.Windows.Forms;
using Microsoft.Win32;

namespace PulsarInstalador
{
    enum Tela { Inicio, Progresso, Pronto, Erro }

    sealed class Opcoes
    {
        public string Atualizar;
        public string Versao = Build.Versao;
        public int Pid;
        public string App;
        public long Tamanho = Build.Tamanho;
        public bool Auto;
        public bool SemAbrir;
        public string Prints;

        public bool ModoAtualizar { get { return Atualizar != null; } }

        public static Opcoes Ler(string[] a)
        {
            Opcoes o = new Opcoes();
            for (int i = 0; i < a.Length; i++)
            {
                string v = i + 1 < a.Length ? a[i + 1] : null;
                switch (a[i])
                {
                    case "--atualizar": o.Atualizar = v; i++; break;
                    case "--versao": if (!string.IsNullOrEmpty(v)) o.Versao = v; i++; break;
                    case "--pid": int.TryParse(v, out o.Pid); i++; break;
                    case "--app": o.App = v; i++; break;
                    case "--tamanho": long.TryParse(v, out o.Tamanho); i++; break;
                    case "--prints": o.Prints = v; i++; break;
                    case "--auto": o.Auto = true; break;
                    case "--sem-abrir": o.SemAbrir = true; break;
                }
            }
            return o;
        }
    }

    /// <summary>Tudo o que a janela mostra. A mesma descrição desenha a janela e os prints do teste.</summary>
    sealed class Estado
    {
        public Tela Tela;
        public bool Atualizacao;
        public string Titulo = "";
        public string Sub = "";
        public string Detalhe = "";
        public string Status = "";
        public double Progresso;
        public string Botao = "";
        public string Secundario = "";
        public string Hover = "";
        public bool PodeFechar = true;
    }

    static class Programa
    {
        public static readonly string Log = Path.Combine(Path.GetTempPath(), "PulsarClient-instalador.log");

        [STAThread]
        static int Main(string[] args)
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Opcoes o = Opcoes.Ler(args);
            Registrar("Início: " + string.Join(" ", args));
            if (o.Prints != null)
            {
                Prints.Gerar(o.Prints);
                return 0;
            }
            Janela j = new Janela(o);
            Application.Run(j);
            Registrar("Fim, código " + j.CodigoSaida);
            return j.CodigoSaida;
        }

        public static void Registrar(string linha)
        {
            try { File.AppendAllText(Log, DateTime.Now.ToString("HH:mm:ss") + "  " + linha + Environment.NewLine); }
            catch { }
        }
    }

    /// <summary>Onde o Pulsar está (ou vai ficar) instalado e como chamar o instalador do electron-builder.</summary>
    static class Instalacao
    {
        public const string Exe = "Pulsar Client.exe";
        public static string VersaoInstalada;
        public static bool TodosUsuarios;

        public static string Pasta()
        {
            VersaoInstalada = null;
            TodosUsuarios = false;
            string achada = Procurar(RegistryHive.CurrentUser, RegistryView.Default);
            if (achada == null)
            {
                achada = Procurar(RegistryHive.LocalMachine, RegistryView.Registry64) ?? Procurar(RegistryHive.LocalMachine, RegistryView.Registry32);
                if (achada != null) TodosUsuarios = true;
            }
            if (achada != null) return achada;
            // Padrão do instalador do electron-builder para um usuário só.
            return Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Programs", "Pulsar Client");
        }

        static string Procurar(RegistryHive raiz, RegistryView vista)
        {
            try
            {
                using (RegistryKey baseKey = RegistryKey.OpenBaseKey(raiz, vista))
                using (RegistryKey k = baseKey.OpenSubKey(@"Software\Microsoft\Windows\CurrentVersion\Uninstall"))
                {
                    if (k == null) return null;
                    foreach (string nome in k.GetSubKeyNames())
                    {
                        using (RegistryKey s = k.OpenSubKey(nome))
                        {
                            if (s == null) continue;
                            string exibido = s.GetValue("DisplayName") as string;
                            string pasta = s.GetValue("InstallLocation") as string;
                            if (exibido == null || !exibido.StartsWith("Pulsar Client") || string.IsNullOrEmpty(pasta)) continue;
                            pasta = pasta.Trim('"');
                            if (!File.Exists(Path.Combine(pasta, Exe))) continue;
                            VersaoInstalada = s.GetValue("DisplayVersion") as string;
                            return pasta;
                        }
                    }
                }
            }
            catch (Exception e)
            {
                Programa.Registrar("Registro: " + e.Message);
            }
            return null;
        }

        public static long TamanhoPasta(string pasta)
        {
            long total = 0;
            try
            {
                if (!Directory.Exists(pasta)) return 0;
                foreach (string f in Directory.EnumerateFiles(pasta, "*", SearchOption.AllDirectories))
                {
                    try { total += new FileInfo(f).Length; }
                    catch { }
                }
            }
            catch { }
            return total;
        }
    }

    sealed class Janela : Form
    {
        const int W = 560;
        const int H = 360;

        readonly Opcoes op;
        readonly Estado st = new Estado();
        readonly System.Windows.Forms.Timer relogio = new System.Windows.Forms.Timer();
        readonly Stopwatch tempo = Stopwatch.StartNew();
        readonly float escala;
        string pasta;
        Thread trabalho;
        public int CodigoSaida = 1;

        public Janela(Opcoes o)
        {
            op = o;
            Text = "Pulsar Client";
            FormBorderStyle = FormBorderStyle.None;
            StartPosition = FormStartPosition.CenterScreen;
            BackColor = Color.FromArgb(5, 7, 15);
            SetStyle(ControlStyles.AllPaintingInWmPaint | ControlStyles.OptimizedDoubleBuffer | ControlStyles.UserPaint | ControlStyles.ResizeRedraw, true);
            try { Icon = Icon.ExtractAssociatedIcon(Application.ExecutablePath); }
            catch { }
            using (Graphics g = CreateGraphics()) escala = Math.Max(1f, g.DpiX / 96f);
            ClientSize = new Size((int)(W * escala), (int)(H * escala));
            KeyPreview = true;

            pasta = Instalacao.Pasta();
            if (op.ModoAtualizar && !string.IsNullOrEmpty(op.App)) pasta = Path.GetDirectoryName(op.App);
            Programa.Registrar("Pasta: " + pasta + (Instalacao.TodosUsuarios ? " (todos os usuários)" : ""));

            st.Atualizacao = op.ModoAtualizar;
            if (op.ModoAtualizar) Comecar();
            else MostrarInicio();

            relogio.Interval = 33;
            relogio.Tick += delegate { Invalidate(); };
            relogio.Start();
        }

        protected override CreateParams CreateParams
        {
            get
            {
                CreateParams cp = base.CreateParams;
                cp.ClassStyle |= 0x20000; // CS_DROPSHADOW: sombra em volta da janela sem moldura
                return cp;
            }
        }

        protected override void OnHandleCreated(EventArgs e)
        {
            base.OnHandleCreated(e);
            // Cantos arredondados no Windows 11 (no 10 a chamada é ignorada).
            try
            {
                int redondo = 2;
                DwmSetWindowAttribute(Handle, 33, ref redondo, sizeof(int));
            }
            catch { }
        }

        protected override void OnShown(EventArgs e)
        {
            base.OnShown(e);
            if (op.Auto && st.Tela == Tela.Inicio) Comecar();
        }

        void MostrarInicio()
        {
            st.Tela = Tela.Inicio;
            st.Titulo = "Pulsar Client";
            st.Sub = "Cliente PvP para Minecraft 1.8.9  ·  versão " + op.Versao;
            if (Instalacao.VersaoInstalada != null)
            {
                st.Detalhe = "A versão " + Instalacao.VersaoInstalada + " já está instalada. Seus ajustes e mods continuam.";
                st.Botao = Instalacao.VersaoInstalada == op.Versao ? "REINSTALAR" : "ATUALIZAR";
            }
            else
            {
                st.Detalhe = "Vai para " + pasta;
                st.Botao = "INSTALAR";
            }
            st.Secundario = "";
            st.PodeFechar = true;
        }

        void Comecar()
        {
            st.Tela = Tela.Progresso;
            st.Titulo = op.ModoAtualizar ? "Atualizando o Pulsar Client" : "Instalando o Pulsar Client";
            st.Sub = op.ModoAtualizar ? "Versão " + op.Versao + "  ·  o Pulsar abre sozinho no fim" : "Versão " + op.Versao;
            st.Status = "Preparando...";
            st.Progresso = 0;
            st.Botao = "";
            st.Secundario = "";
            st.PodeFechar = false;
            trabalho = new Thread(Instalar);
            trabalho.IsBackground = true;
            trabalho.Start();
        }

        void Na(MethodInvoker f)
        {
            try { if (IsHandleCreated && !IsDisposed) BeginInvoke(f); }
            catch { }
        }

        void Avancar(double p, string status)
        {
            Na(delegate
            {
                st.Progresso = Math.Max(st.Progresso, Math.Min(1, p));
                if (status != null) st.Status = status;
            });
        }

        void Instalar()
        {
            string setup = null;
            bool temporario = false;
            try
            {
                if (op.ModoAtualizar)
                {
                    setup = op.Atualizar;
                    if (!File.Exists(setup)) throw new Exception("O arquivo da atualização não foi encontrado. Abra o Pulsar e tente de novo.");
                    Avancar(0.02, "Esperando o Pulsar fechar...");
                    EsperarLauncher();
                }
                else
                {
                    setup = Path.Combine(Path.GetTempPath(), "PulsarClient-Setup-" + op.Versao + ".exe");
                    temporario = true;
                    Extrair(setup);
                }

                long esperado = op.Tamanho;
                long antes = Instalacao.TamanhoPasta(pasta);
                bool esvaziou = antes < esperado / 2;
                string args = op.ModoAtualizar ? "--updated /S" : "/S";
                if (Instalacao.TodosUsuarios) args += " /allusers";
                if (temporario) EsperarArquivoLivre(setup);
                // Pulsar ainda aberto (launcher, ícone perto do relógio ou partes do Electron) segura os arquivos,
                // e o desinstalador da versão antiga para com "Falha ao desinstalar os arquivos do aplicativo antigo".
                Avancar(0.09, "Fechando o Pulsar aberto...");
                FecharPulsarAberto(pasta);
                Process proc = null;
                Stopwatch t = Stopwatch.StartNew();
                // O instalador do Windows às vezes cai logo no começo (código -1073741819, quase sempre com o antivírus
                // ainda lendo o arquivo novo). Nesse caso ele não instalou nada: espera um pouco e roda de novo.
                for (int tentativa = 1; tentativa <= 3; tentativa++)
                {
                    Programa.Registrar("Rodando " + setup + " " + args + (tentativa > 1 ? " (tentativa " + tentativa + ")" : ""));
                    ProcessStartInfo psi = new ProcessStartInfo(setup, args);
                    psi.UseShellExecute = false;
                    proc = Process.Start(psi);
                    t = Stopwatch.StartNew();
                    AcompanharSetup(proc, t, esperado, antes, ref esvaziou);
                    // 2 = o desinstalador da versão antiga não conseguiu apagar algum arquivo (algo ainda aberto):
                    // fecha de novo o que estiver na pasta e tenta outra vez.
                    if (proc.ExitCode == 2 && tentativa < 3)
                    {
                        Programa.Registrar("Instalador parou com o código 2 (arquivos da versão antiga em uso); fechando o Pulsar e tentando de novo");
                        FecharPulsarAberto(pasta);
                        Thread.Sleep(2000 * tentativa);
                        continue;
                    }
                    if (proc.ExitCode != -1073741819 || tentativa == 3) break;
                    Programa.Registrar("Instalador caiu com o código " + proc.ExitCode + " em " + (int)t.Elapsed.TotalSeconds + " s; tentando de novo");
                    Thread.Sleep(1500 * tentativa);
                    if (temporario) EsperarArquivoLivre(setup);
                }
                Programa.Registrar("Instalador terminou com o código " + proc.ExitCode + " em " + (int)t.Elapsed.TotalSeconds + " s");
                if (proc.ExitCode != 0) throw new Exception("O instalador do Windows parou com o código " + proc.ExitCode + ".");
                string novaPasta = Instalacao.Pasta();
                if (File.Exists(Path.Combine(novaPasta, Instalacao.Exe))) pasta = novaPasta;
                if (!File.Exists(Path.Combine(pasta, Instalacao.Exe))) throw new Exception("O Pulsar não apareceu na pasta " + pasta + ".");
                Avancar(1, "Pronto");
                Na(Concluir);
            }
            catch (Exception e)
            {
                Programa.Registrar("Erro: " + e);
                string msg = e.Message;
                Na(delegate { MostrarErro(msg); });
            }
            finally
            {
                if (temporario && setup != null)
                {
                    try { File.Delete(setup); }
                    catch { }
                }
            }
        }

        /** Espera ninguém mais estar com o arquivo aberto (antivírus lendo o instalador recém-criado), no máximo 5 s. */
        static void EsperarArquivoLivre(string arquivo)
        {
            for (int i = 0; i < 50; i++)
            {
                try
                {
                    using (new FileStream(arquivo, FileMode.Open, FileAccess.Read, FileShare.None)) return;
                }
                catch (IOException)
                {
                    Thread.Sleep(100);
                }
            }
        }

        void AcompanharSetup(Process proc, Stopwatch t, long esperado, long antes, ref bool esvaziou)
        {
                while (!proc.WaitForExit(250))
                {
                    // Na atualização a versão velha sai antes da nova entrar: o tamanho só vale depois que a pasta esvazia.
                    long agora = Instalacao.TamanhoPasta(pasta);
                    if (agora < esperado / 2) esvaziou = true;
                    double porTamanho = esvaziou && esperado > 0 ? (double)agora / esperado : 0;
                    double porTempo = 1 - Math.Exp(-t.Elapsed.TotalSeconds / 14.0);
                    double p = 0.08 + 0.88 * Math.Min(1, Math.Max(porTamanho, porTempo * 0.8));
                    Avancar(p, porTamanho >= 0.98 ? "Criando os atalhos..." : "Copiando os arquivos...");
                }
                proc.WaitForExit();
        }

        void Extrair(string destino)
        {
            Stream rec = Assembly.GetExecutingAssembly().GetManifestResourceStream("setup.exe");
            if (rec == null) throw new Exception("Este instalador está incompleto. Baixe de novo.");
            using (rec)
            using (FileStream f = new FileStream(destino, FileMode.Create, FileAccess.Write))
            {
                byte[] buf = new byte[1 << 20];
                long total = Math.Max(1, rec.Length);
                long lidos = 0;
                int n;
                while ((n = rec.Read(buf, 0, buf.Length)) > 0)
                {
                    f.Write(buf, 0, n);
                    lidos += n;
                    Avancar(0.08 * lidos / total, "Preparando...");
                }
            }
        }

        /** Fecha todo programa que está rodando de dentro da pasta do Pulsar (menos este instalador). */
        static void FecharPulsarAberto(string pasta)
        {
            string raiz = string.IsNullOrEmpty(pasta) ? null : Path.GetFullPath(pasta).TrimEnd('\\') + "\\";
            int eu = Process.GetCurrentProcess().Id;
            List<Process> abertos = new List<Process>();
            foreach (Process p in Process.GetProcesses())
            {
                if (p.Id == eu) { p.Dispose(); continue; }
                bool doPulsar = false;
                try
                {
                    if (p.ProcessName.Equals("Pulsar Client", StringComparison.OrdinalIgnoreCase)) doPulsar = true;
                    else if (raiz != null)
                    {
                        string exe = p.MainModule.FileName;
                        doPulsar = exe != null && exe.StartsWith(raiz, StringComparison.OrdinalIgnoreCase);
                    }
                }
                catch { }
                if (doPulsar) abertos.Add(p);
                else p.Dispose();
            }
            if (abertos.Count == 0) return;
            foreach (Process p in abertos)
            {
                try
                {
                    Programa.Registrar("Fechando " + p.ProcessName + " (" + p.Id + ")");
                    p.CloseMainWindow();
                }
                catch { }
            }
            Thread.Sleep(1500);
            foreach (Process p in abertos)
            {
                try
                {
                    if (!p.HasExited)
                    {
                        p.Kill();
                        p.WaitForExit(5000);
                    }
                }
                catch (Exception e) { Programa.Registrar("Não deu para fechar " + p.Id + ": " + e.Message); }
                finally { p.Dispose(); }
            }
            Thread.Sleep(1000);
        }

        void EsperarLauncher()
        {
            if (op.Pid > 0)
            {
                try
                {
                    using (Process p = Process.GetProcessById(op.Pid)) p.WaitForExit(30000);
                }
                catch (ArgumentException) { }
                catch (Exception e) { Programa.Registrar("Esperando o launcher: " + e.Message); }
            }
            // As outras partes do Electron (GPU, telas) fecham logo depois do processo principal.
            Thread.Sleep(1500);
        }

        void Concluir()
        {
            CodigoSaida = 0;
            st.PodeFechar = true;
            if (op.ModoAtualizar)
            {
                st.Tela = Tela.Pronto;
                st.Titulo = "Atualizado!";
                st.Sub = "Pulsar Client " + op.Versao;
                st.Detalhe = op.SemAbrir ? "" : "Abrindo o Pulsar...";
                st.Botao = "";
                if (!op.SemAbrir) AbrirPulsar();
                FecharDepois(op.Auto ? 300 : 1800);
                return;
            }
            st.Tela = Tela.Pronto;
            st.Titulo = "Tudo pronto!";
            st.Sub = "Pulsar Client " + op.Versao + " instalado";
            st.Detalhe = "O atalho do Pulsar está na área de trabalho e no menu Iniciar.";
            st.Botao = "ABRIR O PULSAR";
            st.Secundario = "Fechar";
            if (op.Auto) FecharDepois(300);
        }

        void MostrarErro(string msg)
        {
            CodigoSaida = 1;
            st.Tela = Tela.Erro;
            st.Titulo = op.ModoAtualizar ? "Não deu para atualizar" : "Não deu para instalar";
            st.Sub = msg;
            st.Detalhe = "Detalhes em " + Programa.Log;
            st.Botao = "TENTAR DE NOVO";
            st.Secundario = "Fechar";
            st.PodeFechar = true;
            if (op.Auto) FecharDepois(300);
        }

        void FecharDepois(int ms)
        {
            System.Windows.Forms.Timer t = new System.Windows.Forms.Timer();
            t.Interval = ms;
            t.Tick += delegate { t.Stop(); Close(); };
            t.Start();
        }

        void AbrirPulsar()
        {
            try
            {
                string exe = Path.Combine(pasta, Instalacao.Exe);
                ProcessStartInfo psi = new ProcessStartInfo(exe);
                psi.UseShellExecute = true;
                psi.WorkingDirectory = pasta;
                Process.Start(psi);
            }
            catch (Exception e)
            {
                Programa.Registrar("Abrir o Pulsar: " + e.Message);
            }
        }

        void Clicar(string alvo)
        {
            if (alvo == "fechar")
            {
                if (st.PodeFechar) Close();
                return;
            }
            if (alvo == "minimizar")
            {
                WindowState = FormWindowState.Minimized;
                return;
            }
            if (alvo == "secundario")
            {
                Close();
                return;
            }
            if (alvo != "botao") return;
            if (st.Tela == Tela.Inicio || st.Tela == Tela.Erro) Comecar();
            else if (st.Tela == Tela.Pronto)
            {
                AbrirPulsar();
                Close();
            }
        }

        // ---------- mouse e teclado ----------

        PointF Logico(Point p)
        {
            return new PointF(p.X / escala, p.Y / escala);
        }

        protected override void OnMouseMove(MouseEventArgs e)
        {
            base.OnMouseMove(e);
            string h = Desenho.Alvo(st, Logico(e.Location));
            if (h != st.Hover)
            {
                st.Hover = h;
                Cursor = h != "" ? Cursors.Hand : Cursors.Default;
            }
        }

        protected override void OnMouseLeave(EventArgs e)
        {
            base.OnMouseLeave(e);
            st.Hover = "";
        }

        protected override void OnMouseDown(MouseEventArgs e)
        {
            base.OnMouseDown(e);
            if (e.Button != MouseButtons.Left) return;
            string alvo = Desenho.Alvo(st, Logico(e.Location));
            if (alvo != "")
            {
                Clicar(alvo);
                return;
            }
            // Arrastar a janela por qualquer parte vazia.
            ReleaseCapture();
            SendMessage(Handle, 0xA1, (IntPtr)2, IntPtr.Zero);
        }

        protected override void OnKeyDown(KeyEventArgs e)
        {
            base.OnKeyDown(e);
            if (e.KeyCode == Keys.Enter && st.Botao != "") Clicar("botao");
            else if (e.KeyCode == Keys.Escape) Clicar("fechar");
        }

        protected override void OnFormClosing(FormClosingEventArgs e)
        {
            // Fechar no meio da instalação deixaria o Pulsar pela metade.
            if (!st.PodeFechar && e.CloseReason == CloseReason.UserClosing) e.Cancel = true;
            base.OnFormClosing(e);
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            Graphics g = e.Graphics;
            g.ScaleTransform(escala, escala);
            Desenho.Pintar(g, st, (float)tempo.Elapsed.TotalSeconds);
        }

        [DllImport("user32.dll")] static extern bool ReleaseCapture();
        [DllImport("user32.dll")] static extern IntPtr SendMessage(IntPtr hWnd, int msg, IntPtr wParam, IntPtr lParam);
        [DllImport("dwmapi.dll")] static extern int DwmSetWindowAttribute(IntPtr hwnd, int attr, ref int value, int size);
    }

    /// <summary>Desenho da janela em coordenadas de 560 x 360 (a escala da tela é aplicada antes).</summary>
    static class Desenho
    {
        const float W = 560;
        const float H = 360;
        static readonly Color Fundo1 = Color.FromArgb(13, 20, 48);
        static readonly Color Fundo2 = Color.FromArgb(5, 7, 15);
        static readonly Color Linha = Color.FromArgb(46, 140, 170, 255);
        static readonly Color Texto = Color.FromArgb(233, 241, 255);
        static readonly Color Apagado = Color.FromArgb(141, 153, 187);
        static readonly Color Escuro = Color.FromArgb(91, 103, 137);
        static readonly Color Feixe = Color.FromArgb(127, 231, 255);
        static readonly Color Brasa = Color.FromArgb(255, 138, 61);
        static readonly Color Brasa2 = Color.FromArgb(255, 179, 107);
        static readonly Color Ruim = Color.FromArgb(255, 107, 107);
        static readonly Color Ok = Color.FromArgb(110, 231, 168);

        static readonly string[] Dicas =
        {
            "Dica: no jogo, Shift direito abre o editor da HUD.",
            "Dica: a aba Texturas tem 5 pacotes para PvP e 5 de qualidade.",
            "Dica: no Esc do jogo, Multiplayer troca de servidor sem sair.",
            "Dica: o Motor Pulsar já vem no modo desempenho.",
            "Dica: Atalhos de Texto mandam um comando com uma tecla.",
        };

        static Image logo;
        static PrivateFontCollection fontes;
        static FontFamily familia;
        static readonly List<float[]> estrelas = new List<float[]>();

        static Desenho()
        {
            Assembly a = Assembly.GetExecutingAssembly();
            try
            {
                using (Stream s = a.GetManifestResourceStream("logo.png"))
                    if (s != null) logo = new Bitmap(Image.FromStream(s));
            }
            catch { }
            try
            {
                fontes = new PrivateFontCollection();
                foreach (string nome in new[] { "Exo2-Regular.ttf", "Exo2-Bold.ttf" })
                {
                    using (Stream s = a.GetManifestResourceStream(nome))
                    {
                        if (s == null) continue;
                        byte[] dados = new byte[s.Length];
                        s.Read(dados, 0, dados.Length);
                        // A memória da fonte precisa ficar viva enquanto o programa roda.
                        IntPtr mem = Marshal.AllocCoTaskMem(dados.Length);
                        Marshal.Copy(dados, 0, mem, dados.Length);
                        fontes.AddMemoryFont(mem, dados.Length);
                    }
                }
                if (fontes.Families.Length > 0) familia = fontes.Families[0];
            }
            catch { }
            if (familia == null) familia = new FontFamily("Segoe UI");
            Random r = new Random(7);
            for (int i = 0; i < 150; i++)
                estrelas.Add(new float[] { (float)r.NextDouble() * W, (float)r.NextDouble() * H, (float)r.NextDouble(), (float)r.NextDouble() * 6.28f });
        }

        static Font Fonte(float px, bool negrito)
        {
            FontStyle estilo = negrito && familia.IsStyleAvailable(FontStyle.Bold) ? FontStyle.Bold : FontStyle.Regular;
            return new Font(familia, px, estilo, GraphicsUnit.Pixel);
        }

        // Áreas clicáveis (as mesmas usadas no desenho).
        static RectangleF Fechar { get { return new RectangleF(W - 40, 8, 30, 26); } }
        static RectangleF Minimizar { get { return new RectangleF(W - 72, 8, 30, 26); } }
        static RectangleF Botao(Estado s)
        {
            float w = s.Botao.Length > 10 ? 236 : 200;
            return new RectangleF((W - w) / 2, s.Tela == Tela.Erro ? 262 : 258, w, 46);
        }
        static RectangleF Secundario(Estado s)
        {
            RectangleF b = Botao(s);
            return new RectangleF(W / 2 - 50, b.Bottom + 8, 100, 22);
        }

        public static string Alvo(Estado s, PointF p)
        {
            if (Fechar.Contains(p) && s.PodeFechar) return "fechar";
            if (Minimizar.Contains(p)) return "minimizar";
            if (s.Botao != "" && Botao(s).Contains(p)) return "botao";
            if (s.Secundario != "" && Secundario(s).Contains(p)) return "secundario";
            return "";
        }

        public static void Pintar(Graphics g, Estado s, float t)
        {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.InterpolationMode = InterpolationMode.HighQualityBicubic;
            g.PixelOffsetMode = PixelOffsetMode.HighQuality;
            g.TextRenderingHint = TextRenderingHint.AntiAliasGridFit;

            Ceu(g, t);
            Topo(g, s);
            float cy = s.Tela == Tela.Progresso ? 104 : 100;
            Pulsar(g, W / 2, cy, s.Tela == Tela.Progresso ? 92 : 104, t, s.Tela == Tela.Progresso ? 2.2f : 0.9f);

            Color corTitulo = s.Tela == Tela.Erro ? Ruim : Texto;
            using (Font f = Fonte(s.Tela == Tela.Inicio ? 30 : 24, true))
                Centro(g, s.Titulo, f, corTitulo, 172);

            switch (s.Tela)
            {
                case Tela.Inicio:
                    using (Font f = Fonte(14, false)) Centro(g, s.Sub, f, Apagado, 210);
                    using (Font f = Fonte(12, false)) Centro(g, Encurtar(g, s.Detalhe, f, 440), f, Escuro, 232);
                    BotaoPrincipal(g, s);
                    break;
                case Tela.Progresso:
                    using (Font f = Fonte(14, false)) Centro(g, s.Sub, f, Apagado, 205);
                    Barra(g, s, t);
                    using (Font f = Fonte(12, false))
                        Centro(g, Dicas[(int)(t / 4.5f) % Dicas.Length], f, Escuro, 302);
                    break;
                case Tela.Pronto:
                    using (Font f = Fonte(14, false)) Centro(g, s.Sub, f, Ok, 205);
                    using (Font f = Fonte(12, false)) Centro(g, s.Detalhe, f, Apagado, 228);
                    if (s.Botao != "") BotaoPrincipal(g, s);
                    break;
                case Tela.Erro:
                    using (Font f = Fonte(13, false))
                    {
                        RectangleF caixa = new RectangleF(60, 200, W - 120, 40);
                        using (StringFormat sf = new StringFormat())
                        {
                            sf.Alignment = StringAlignment.Center;
                            sf.LineAlignment = StringAlignment.Center;
                            sf.Trimming = StringTrimming.EllipsisCharacter;
                            using (SolidBrush b = new SolidBrush(Texto)) g.DrawString(s.Sub, f, b, caixa, sf);
                        }
                    }
                    using (Font f = Fonte(11, false)) Centro(g, Encurtar(g, s.Detalhe, f, 460), f, Escuro, 244);
                    BotaoPrincipal(g, s);
                    break;
            }
            if (s.Secundario != "")
            {
                RectangleF r = Secundario(s);
                using (Font f = Fonte(13, false))
                    Centro(g, s.Secundario, f, s.Hover == "secundario" ? Texto : Apagado, r.Y + 2);
            }

            using (Pen p = new Pen(Linha, 1)) g.DrawRectangle(p, 0, 0, W - 1, H - 1);
        }

        static void Ceu(Graphics g, float t)
        {
            using (LinearGradientBrush b = new LinearGradientBrush(new RectangleF(0, 0, W, H), Fundo1, Fundo2, 90f))
                g.FillRectangle(b, 0, 0, W, H);
            // Nebulosa suave atrás do pulsar.
            using (GraphicsPath p = new GraphicsPath())
            {
                p.AddEllipse(W / 2 - 260, -40, 520, 300);
                using (PathGradientBrush b = new PathGradientBrush(p))
                {
                    b.CenterColor = Color.FromArgb(46, 120, 110, 255);
                    b.SurroundColors = new[] { Color.FromArgb(0, 120, 110, 255) };
                    g.FillPath(b, p);
                }
            }
            foreach (float[] e in estrelas)
            {
                float brilho = 0.35f + 0.65f * (0.5f + 0.5f * (float)Math.Sin(t * (0.6f + e[2]) + e[3]));
                int a = (int)(40 + 150 * e[2] * brilho);
                Color c = e[2] > 0.93f ? Color.FromArgb(a, 200, 240, 255) : e[2] > 0.85f ? Color.FromArgb(a, 190, 170, 255) : Color.FromArgb(a, 233, 241, 255);
                float tam = e[2] > 0.95f ? 2f : 1.2f;
                using (SolidBrush b = new SolidBrush(c)) g.FillRectangle(b, e[0], e[1], tam, tam);
            }
        }

        static void Topo(Graphics g, Estado s)
        {
            using (Font f = Fonte(11, true))
            {
                float x = 18;
                // "PULSAR CLIENT" espaçado, como no launcher.
                using (SolidBrush b = new SolidBrush(Apagado))
                {
                    foreach (char c in (s.Atualizacao ? "PULSAR CLIENT  ·  ATUALIZAÇÃO" : "PULSAR CLIENT  ·  INSTALAÇÃO"))
                    {
                        string l = c.ToString();
                        g.DrawString(l, f, b, x, 14, StringFormat.GenericTypographic);
                        x += (c == ' ' ? 3.5f : g.MeasureString(l, f, 100, StringFormat.GenericTypographic).Width) + 1.6f;
                    }
                }
            }
            using (Pen p = new Pen(Color.FromArgb(s.Hover == "minimizar" ? 255 : 170, Apagado), 1.4f))
                g.DrawLine(p, Minimizar.X + 10, Minimizar.Y + 13.5f, Minimizar.Right - 10, Minimizar.Y + 13.5f);
            if (s.Hover == "fechar")
                using (SolidBrush b = new SolidBrush(Color.FromArgb(60, 255, 107, 107))) g.FillRectangle(b, Fechar);
            using (Pen p = new Pen(Color.FromArgb(s.PodeFechar ? (s.Hover == "fechar" ? 255 : 170) : 50, s.Hover == "fechar" ? Texto : Apagado), 1.4f))
            {
                RectangleF r = Fechar;
                g.DrawLine(p, r.X + 10, r.Y + 8, r.Right - 10, r.Bottom - 8);
                g.DrawLine(p, r.Right - 10, r.Y + 8, r.X + 10, r.Bottom - 8);
            }
        }

        /// <summary>Logo "PC" com os dois feixes do pulsar girando atrás e o brilho batendo no ritmo.</summary>
        static void Pulsar(Graphics g, float cx, float cy, float tam, float t, float giro)
        {
            float pulso = (float)Math.Pow(Math.Max(0, Math.Sin(t * 5.5)), 6);
            for (int i = 6; i >= 1; i--)
            {
                float r = tam / 2 + i * 5 + pulso * 6;
                using (SolidBrush b = new SolidBrush(Color.FromArgb((int)(10 + pulso * 10), Feixe)))
                    g.FillEllipse(b, cx - r, cy - r, r * 2, r * 2);
            }
            double ang = t * giro - 0.56;
            for (int lado = -1; lado <= 1; lado += 2)
            {
                float dx = (float)Math.Cos(ang) * lado;
                float dy = (float)Math.Sin(ang) * lado;
                // Os feixes somem antes de chegar no título.
                const int passos = 22;
                for (int i = 0; i < passos; i++)
                {
                    float a0 = tam * 0.5f + i * 5.2f;
                    float a1 = a0 + 5.6f;
                    int alfa = (int)(150 * Math.Pow(1 - i / (float)passos, 2.2));
                    using (Pen p = new Pen(Color.FromArgb(alfa, 200, 247, 255), 2.2f + i * 0.5f))
                        g.DrawLine(p, cx + dx * a0, cy + dy * a0, cx + dx * a1, cy + dy * a1);
                }
            }
            if (logo != null) g.DrawImage(logo, cx - tam / 2, cy - tam / 2, tam, tam);
        }

        static void BotaoPrincipal(Graphics g, Estado s)
        {
            RectangleF r = Botao(s);
            bool hover = s.Hover == "botao";
            using (GraphicsPath p = Arredondado(r, 9))
            {
                Color c1 = hover ? Color.FromArgb(255, 196, 132) : Brasa2;
                Color c2 = hover ? Color.FromArgb(255, 152, 80) : Brasa;
                // Brilho em volta do botão.
                for (int i = 4; i >= 1; i--)
                {
                    RectangleF rr = RectangleF.Inflate(r, i * 2.5f, i * 2.5f);
                    using (GraphicsPath pp = Arredondado(rr, 9 + i * 2.5f))
                    using (SolidBrush b = new SolidBrush(Color.FromArgb(hover ? 22 : 14, Brasa)))
                        g.FillPath(b, pp);
                }
                using (LinearGradientBrush b = new LinearGradientBrush(r, c1, c2, 90f)) g.FillPath(b, p);
            }
            using (Font f = Fonte(16, true))
            using (SolidBrush b = new SolidBrush(Color.FromArgb(26, 11, 2)))
            using (StringFormat sf = new StringFormat())
            {
                sf.Alignment = StringAlignment.Center;
                sf.LineAlignment = StringAlignment.Center;
                g.DrawString(s.Botao, f, b, new RectangleF(r.X, r.Y + 1, r.Width, r.Height), sf);
            }
        }

        static void Barra(Graphics g, Estado s, float t)
        {
            RectangleF trilho = new RectangleF(90, 262, W - 180, 8);
            using (Font f = Fonte(13, false))
            using (SolidBrush b = new SolidBrush(Apagado))
                g.DrawString(s.Status, f, b, trilho.X, trilho.Y - 24);
            using (Font f = Fonte(13, true))
            using (SolidBrush b = new SolidBrush(Feixe))
            using (StringFormat sf = new StringFormat())
            {
                sf.Alignment = StringAlignment.Far;
                g.DrawString((int)Math.Round(s.Progresso * 100) + "%", f, b, new RectangleF(trilho.X, trilho.Y - 24, trilho.Width, 20), sf);
            }
            using (GraphicsPath p = Arredondado(trilho, 4))
            using (SolidBrush b = new SolidBrush(Color.FromArgb(22, 33, 74)))
                g.FillPath(b, p);
            float w = Math.Max(8, trilho.Width * (float)s.Progresso);
            RectangleF cheio = new RectangleF(trilho.X, trilho.Y, w, trilho.Height);
            for (int i = 3; i >= 1; i--)
            {
                using (GraphicsPath pp = Arredondado(RectangleF.Inflate(cheio, i * 2, i * 2), 4 + i * 2))
                using (SolidBrush b = new SolidBrush(Color.FromArgb(18, Feixe)))
                    g.FillPath(b, pp);
            }
            using (GraphicsPath p = Arredondado(cheio, 4))
            using (LinearGradientBrush b = new LinearGradientBrush(new RectangleF(trilho.X - 1, 0, trilho.Width + 2, 1), Color.FromArgb(155, 124, 255), Feixe, 0f))
                g.FillPath(b, p);
            // Faísca correndo dentro da parte cheia.
            float x = cheio.X + (t * 160 % (cheio.Width + 60)) - 30;
            RectangleF brilho = new RectangleF(x, cheio.Y, 30, cheio.Height);
            brilho.Intersect(cheio);
            if (brilho.Width > 1)
                using (LinearGradientBrush b = new LinearGradientBrush(new RectangleF(x - 1, 0, 32, 1), Color.FromArgb(0, 255, 255, 255), Color.FromArgb(0, 255, 255, 255), 0f))
                {
                    ColorBlend cb = new ColorBlend();
                    cb.Colors = new[] { Color.FromArgb(0, 255, 255, 255), Color.FromArgb(170, 255, 255, 255), Color.FromArgb(0, 255, 255, 255) };
                    cb.Positions = new[] { 0f, 0.5f, 1f };
                    b.InterpolationColors = cb;
                    g.FillRectangle(b, brilho);
                }
        }

        static void Centro(Graphics g, string texto, Font f, Color cor, float y)
        {
            using (SolidBrush b = new SolidBrush(cor))
            using (StringFormat sf = new StringFormat())
            {
                sf.Alignment = StringAlignment.Center;
                g.DrawString(texto, f, b, new RectangleF(0, y, W, f.Size * 1.6f), sf);
            }
        }

        static string Encurtar(Graphics g, string texto, Font f, float max)
        {
            if (g.MeasureString(texto, f).Width <= max) return texto;
            while (texto.Length > 8 && g.MeasureString("..." + texto, f).Width > max) texto = texto.Substring(1);
            return "..." + texto;
        }

        static GraphicsPath Arredondado(RectangleF r, float raio)
        {
            float d = Math.Min(raio * 2, Math.Min(r.Width, r.Height));
            GraphicsPath p = new GraphicsPath();
            p.AddArc(r.X, r.Y, d, d, 180, 90);
            p.AddArc(r.Right - d, r.Y, d, d, 270, 90);
            p.AddArc(r.Right - d, r.Bottom - d, d, d, 0, 90);
            p.AddArc(r.X, r.Bottom - d, d, d, 90, 90);
            p.CloseFigure();
            return p;
        }
    }

    /// <summary>Prints de todas as telas, para conferir o visual no CI (--prints pasta).</summary>
    static class Prints
    {
        public static void Gerar(string pasta)
        {
            Directory.CreateDirectory(pasta);
            string dir = @"C:\Users\Rick\AppData\Local\Programs\Pulsar Client";

            Estado s = new Estado { Tela = Tela.Inicio, Titulo = "Pulsar Client", Sub = "Cliente PvP para Minecraft 1.8.9  ·  versão " + Build.Versao, Detalhe = "Vai para " + dir, Botao = "INSTALAR" };
            Salvar(s, pasta, "instalador-inicio", 1.2f);
            s.Hover = "botao";
            Salvar(s, pasta, "instalador-inicio-hover", 1.2f);

            s = new Estado { Tela = Tela.Inicio, Titulo = "Pulsar Client", Sub = "Cliente PvP para Minecraft 1.8.9  ·  versão " + Build.Versao, Detalhe = "A versão 0.5.0 já está instalada. Seus ajustes e mods continuam.", Botao = "ATUALIZAR" };
            Salvar(s, pasta, "instalador-reinstalar", 1.2f);

            s = new Estado { Tela = Tela.Progresso, Titulo = "Instalando o Pulsar Client", Sub = "Versão " + Build.Versao, Status = "Copiando os arquivos...", Progresso = 0.42, PodeFechar = false };
            Salvar(s, pasta, "instalador-progresso", 3.4f);

            s = new Estado { Tela = Tela.Pronto, Titulo = "Tudo pronto!", Sub = "Pulsar Client " + Build.Versao + " instalado", Detalhe = "O atalho do Pulsar está na área de trabalho e no menu Iniciar.", Botao = "ABRIR O PULSAR", Secundario = "Fechar" };
            Salvar(s, pasta, "instalador-pronto", 1.2f);

            s = new Estado { Tela = Tela.Erro, Titulo = "Não deu para instalar", Sub = "O instalador do Windows parou com o código 2.", Detalhe = "Detalhes em " + Programa.Log, Botao = "TENTAR DE NOVO", Secundario = "Fechar" };
            Salvar(s, pasta, "instalador-erro", 1.2f);

            s = new Estado { Tela = Tela.Progresso, Atualizacao = true, Titulo = "Atualizando o Pulsar Client", Sub = "Versão " + Build.Versao + "  ·  o Pulsar abre sozinho no fim", Status = "Copiando os arquivos...", Progresso = 0.67, PodeFechar = false };
            Salvar(s, pasta, "atualizador-progresso", 8.1f);

            s = new Estado { Tela = Tela.Pronto, Atualizacao = true, Titulo = "Atualizado!", Sub = "Pulsar Client " + Build.Versao, Detalhe = "Abrindo o Pulsar..." };
            Salvar(s, pasta, "atualizador-pronto", 1.2f);
        }

        static void Salvar(Estado s, string pasta, string nome, float t)
        {
            const float escala = 1.5f;
            using (Bitmap bmp = new Bitmap((int)(560 * escala), (int)(360 * escala), PixelFormat.Format32bppArgb))
            using (Graphics g = Graphics.FromImage(bmp))
            {
                g.ScaleTransform(escala, escala);
                Desenho.Pintar(g, s, t);
                bmp.Save(Path.Combine(pasta, nome + ".png"), ImageFormat.Png);
            }
        }
    }
}
