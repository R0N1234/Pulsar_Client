package dev.pulsarclient.mods;

import dev.pulsarclient.settings.BoolSetting;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import javax.imageio.ImageIO;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Capturas: depois de cada print (F2), o chat mostra botões para abrir a imagem, copiar e abrir a pasta.
 * Com "Copiar automaticamente", a print já vai para a área de transferência: é só colar no Discord.
 * Não fica olhando a pasta toda hora: só confere quando a pasta muda ou quando a tecla de print é apertada.
 */
public class Capturas extends PulsarMod {
    private final BoolSetting copiarAuto = add(new BoolSetting("copiar_auto", "Copiar a print automaticamente", true));
    private final BoolSetting botoes = add(new BoolSetting("botoes", "Botões no chat (abrir, copiar, pasta)", true));

    private long pastaMudou = -1;
    private long desde;
    private boolean conferir;
    private int ticks;
    private final Set<String> avisadas = new HashSet<String>();

    public Capturas() {
        super("capturas", "Capturas", "Copia a print na hora e mostra botões no chat", Category.SOCIAL, true);
        ClientCommandHandler.instance.registerCommand(new Comando());
    }

    private File pasta() {
        return new File(mc.mcDataDir, "screenshots");
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        if (Keyboard.getEventKeyState() && Keyboard.getEventKey() == mc.gameSettings.keyBindScreenshot.getKeyCode()) conferir = true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (pastaMudou < 0) {
            // Prints de antes do jogo abrir não contam.
            pastaMudou = pasta().lastModified();
            desde = System.currentTimeMillis() - 1000;
            return;
        }
        if (!isEnabled() || mc.thePlayer == null) {
            conferir = false;
            return;
        }
        if (++ticks % 10 == 0) {
            long agora = pasta().lastModified();
            if (agora != pastaMudou) {
                pastaMudou = agora;
                conferir = true;
            }
        }
        if (!conferir) return;
        conferir = false;
        File[] arquivos = pasta().listFiles();
        if (arquivos == null) return;
        for (File f : arquivos) {
            if (!f.getName().toLowerCase().endsWith(".png") || f.lastModified() < desde || !avisadas.add(f.getName())) continue;
            novaPrint(f);
        }
    }

    /** Uma print nova na pasta: copia (se escolhido) e mostra os botões. */
    public void novaPrint(File f) {
        System.out.println("[Pulsar] Capturas: print " + f.getName());
        if (copiarAuto.get()) copiar(f, false);
        if (!botoes.get() || mc.thePlayer == null) return;
        IChatComponent linha = new ChatComponentText(EnumChatFormatting.LIGHT_PURPLE + "[Capturas] " + EnumChatFormatting.GRAY + (copiarAuto.get() ? "Print copiada. " : "Print salva. "));
        linha.appendSibling(botao("Abrir", new ClickEvent(ClickEvent.Action.OPEN_FILE, f.getAbsolutePath()), "Abrir " + f.getName()));
        linha.appendSibling(new ChatComponentText(" "));
        linha.appendSibling(botao("Copiar", new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pulsarcaptura copiar " + f.getName()), "Copiar a imagem para colar no Discord"));
        linha.appendSibling(new ChatComponentText(" "));
        linha.appendSibling(botao("Pasta", new ClickEvent(ClickEvent.Action.OPEN_FILE, pasta().getAbsolutePath()), "Abrir a pasta das prints"));
        mc.thePlayer.addChatMessage(linha);
    }

    private static IChatComponent botao(String texto, ClickEvent clique, String dica) {
        IChatComponent c = new ChatComponentText("[" + texto + "]");
        c.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.AQUA).setBold(true).setChatClickEvent(clique)
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(dica))));
        return c;
    }

    /** Copia a imagem para a área de transferência numa linha separada (ler o PNG pode levar alguns milissegundos). */
    private void copiar(final File f, final boolean avisar) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                String resultado;
                try {
                    if (GraphicsEnvironment.isHeadless()) throw new IllegalStateException("sem área de transferência");
                    final BufferedImage img = ImageIO.read(f);
                    if (img == null) throw new IllegalStateException("imagem vazia");
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new Imagem(img), null);
                    resultado = "copiada";
                } catch (Throwable e) {
                    resultado = "não deu para copiar (" + e.getMessage() + ")";
                }
                System.out.println("[Pulsar] Capturas: " + f.getName() + " " + resultado);
                if (avisar && mc.thePlayer != null) {
                    final String texto = resultado;
                    mc.addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                            if (mc.thePlayer != null) mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.LIGHT_PURPLE + "[Capturas] " + EnumChatFormatting.GRAY + "Print " + texto + "."));
                        }
                    });
                }
            }
        }, "Pulsar - capturas");
        t.setDaemon(true);
        t.start();
    }

    private static final class Imagem implements Transferable {
        private final Image img;

        Imagem(Image img) {
            this.img = img;
        }

        @Override public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[] {DataFlavor.imageFlavor}; }
        @Override public boolean isDataFlavorSupported(DataFlavor flavor) { return DataFlavor.imageFlavor.equals(flavor); }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
            return img;
        }
    }

    /** /pulsarcaptura copiar <arquivo>: o botão Copiar do chat chama este comando (só no seu jogo, nada vai para o servidor). */
    private final class Comando extends CommandBase {
        @Override public String getCommandName() { return "pulsarcaptura"; }
        @Override public String getCommandUsage(ICommandSender sender) { return "/pulsarcaptura copiar <arquivo>"; }
        @Override public int getRequiredPermissionLevel() { return 0; }
        @Override public boolean canCommandSenderUseCommand(ICommandSender sender) { return true; }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length < 2 || !args[0].equals("copiar")) return;
            StringBuilder nome = new StringBuilder(args[1]);
            for (int i = 2; i < args.length; i++) nome.append(' ').append(args[i]);
            // Só arquivos da pasta de prints, pelo nome (nada de caminhos).
            File f = new File(pasta(), new File(nome.toString()).getName());
            if (f.isFile()) copiar(f, true);
        }
    }
}
