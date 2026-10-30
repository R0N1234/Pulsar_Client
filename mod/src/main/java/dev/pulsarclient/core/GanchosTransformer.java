package dev.pulsarclient.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Ganchos dos mods do Pulsar dentro do Minecraft (os do Motor ficam no {@link MotorTransformer}):
 *
 * - FontRenderer: a Fonte do Pulsar desenha as letras e mede o texto. Entra tudo ou nada:
 *   se faltar qualquer método, a fonte fica desligada, para desenho e medida nunca discordarem.
 * - ItemRenderer.renderItemInFirstPerson: balanço do braço enquanto bloqueia com a espada (Animações Clássicas).
 * - RendererLivingEntity.setBrightness: cor de quando a entidade leva dano (Impacto).
 *
 * Os nomes no jogo publicado são os nomes SRG (func_..., field_...); os outros são do ambiente de desenvolvimento.
 */
public class GanchosTransformer implements IClassTransformer {
    private static final String G = "dev/pulsarclient/core/Ganchos";
    private static final String FR = "net/minecraft/client/gui/FontRenderer";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || transformedName == null) return bytes;
        try {
            if (transformedName.equals("net.minecraft.client.gui.FontRenderer")) return patch(bytes, transformedName, 0);
            if (transformedName.equals("net.minecraft.client.renderer.ItemRenderer")) return patch(bytes, transformedName, 1);
            if (transformedName.equals("net.minecraft.client.renderer.entity.RendererLivingEntity")) return patch(bytes, transformedName, 2);
        } catch (Throwable t) {
            System.err.println("[Pulsar] Não deu para instalar os ganchos em " + transformedName + ": " + t);
        }
        return bytes;
    }

    private static byte[] patch(byte[] bytes, String className, int which) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        boolean frames = (node.version & 0xFFFF) >= Opcodes.V1_6;
        boolean ok;
        if (which == 0) ok = fonte(node, frames);
        else if (which == 1) ok = balanco(node);
        else ok = corDano(node);
        if (!ok) return bytes;
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    // ---------- fonte ----------

    private static boolean fonte(ClassNode node, boolean frames) {
        MethodNode def = method(node, "(IZ)F", "renderDefaultChar", "func_78266_a");
        MethodNode uni = method(node, "(CZ)F", "renderUnicodeChar", "func_78277_a");
        MethodNode charW = method(node, "(C)I", "getCharWidth", "func_78263_a");
        MethodNode textW = method(node, "(Ljava/lang/String;)I", "getStringWidth", "func_78256_a");
        MethodNode atPos = method(node, "(Ljava/lang/String;Z)V", "renderStringAtPos", "func_78255_a");
        String posX = floatField(node, 0, "posX", "field_78295_j");
        String posY = floatField(node, 1, "posY", "field_78296_k");
        if (def == null || uni == null || charW == null || textW == null || posX == null || posY == null) {
            System.err.println("[Pulsar] Fonte: ganchos não instalados, faltou algo em FontRenderer (" + def + ", " + uni
                    + ", " + charW + ", " + textW + ", " + posX + ", " + posY + "):" + listMethods(node));
            return false;
        }

        // renderDefaultChar(I, Z) e renderUnicodeChar(C, Z): se a Fonte do Pulsar desenhar, devolve a largura dela.
        def.instructions.insert(floatHead(node.name, posX, posY, "caractere", "(L" + FR + ";FFIZ)F", frames));
        uni.instructions.insert(floatHead(node.name, posX, posY, "unicode", "(L" + FR + ";FFCZ)F", frames));

        // getCharWidth(C) e getStringWidth(String).
        InsnList cw = new InsnList();
        cw.add(new VarInsnNode(Opcodes.ALOAD, 0));
        cw.add(new VarInsnNode(Opcodes.ILOAD, 1));
        cw.add(new MethodInsnNode(Opcodes.INVOKESTATIC, G, "larguraCaractere", "(L" + FR + ";C)I", false));
        charW.instructions.insert(intReturn(cw, frames));
        InsnList tw = new InsnList();
        tw.add(new VarInsnNode(Opcodes.ALOAD, 0));
        tw.add(new VarInsnNode(Opcodes.ALOAD, 1));
        tw.add(new MethodInsnNode(Opcodes.INVOKESTATIC, G, "larguraTexto", "(L" + FR + ";Ljava/lang/String;)I", false));
        textW.instructions.insert(intReturn(tw, frames));
        Ganchos.ganchoFonte = true;
        System.out.println("[Pulsar] Fonte: ganchos instalados em FontRenderer (" + def.name + ", " + uni.name + ", " + charW.name + ", " + textW.name + ")");

        // "posX += (float)(int) largura" vira "posX += Ganchos.avanco(largura)". No Forge essa conta fica em doDraw,
        // chamado por renderStringAtPos; no jogo puro fica no próprio renderStringAtPos.
        MethodNode doDraw = method(node, "(F)V", "doDraw");
        List<AbstractInsnNode> casts = new ArrayList<AbstractInsnNode>();
        MethodNode alvo = null;
        for (MethodNode m : new MethodNode[]{doDraw, atPos}) {
            if (m == null) continue;
            for (AbstractInsnNode in = m.instructions.getFirst(); in != null; in = in.getNext()) {
                if (in.getOpcode() == Opcodes.F2I && next(in) != null && next(in).getOpcode() == Opcodes.I2F) {
                    casts.add(in);
                    alvo = m;
                }
            }
        }
        if (casts.size() == 1) {
            AbstractInsnNode f2i = casts.get(0);
            AbstractInsnNode i2f = next(f2i);
            alvo.instructions.set(f2i, new MethodInsnNode(Opcodes.INVOKESTATIC, G, "avanco", "(F)F", false));
            alvo.instructions.remove(i2f);
            Ganchos.ganchoAvanco = true;
            System.out.println("[Pulsar] Fonte: espaçamento com fração instalado (" + alvo.name + ")");
        } else {
            System.err.println("[Pulsar] Fonte: espaçamento com fração não instalado (" + casts.size() + " conversões)");
        }
        return true;
    }

    /** ALOAD this; posX; posY; arg1; arg2; INVOKESTATIC; se >= 0 retorna, senão segue o método original. */
    private static InsnList floatHead(String owner, String posX, String posY, String hook, String desc, boolean frames) {
        InsnList list = new InsnList();
        LabelNode segue = new LabelNode();
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new FieldInsnNode(Opcodes.GETFIELD, owner, posX, "F"));
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new FieldInsnNode(Opcodes.GETFIELD, owner, posY, "F"));
        list.add(new VarInsnNode(Opcodes.ILOAD, 1));
        list.add(new VarInsnNode(Opcodes.ILOAD, 2));
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, G, hook, desc, false));
        list.add(new InsnNode(Opcodes.DUP));
        list.add(new InsnNode(Opcodes.FCONST_0));
        list.add(new InsnNode(Opcodes.FCMPL));
        list.add(new JumpInsnNode(Opcodes.IFLT, segue));
        list.add(new InsnNode(Opcodes.FRETURN));
        list.add(segue);
        if (frames) list.add(new FrameNode(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.FLOAT}));
        list.add(new InsnNode(Opcodes.POP));
        return list;
    }

    /** Depois da chamada já na pilha: se não for Ganchos.NAO, retorna o valor; senão segue o método original. */
    private static InsnList intReturn(InsnList call, boolean frames) {
        LabelNode segue = new LabelNode();
        call.add(new InsnNode(Opcodes.DUP));
        call.add(new LdcInsnNode(Integer.valueOf(Ganchos.NAO)));
        call.add(new JumpInsnNode(Opcodes.IF_ICMPEQ, segue));
        call.add(new InsnNode(Opcodes.IRETURN));
        call.add(segue);
        if (frames) call.add(new FrameNode(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER}));
        call.add(new InsnNode(Opcodes.POP));
        return call;
    }

    // ---------- animações clássicas ----------

    private static boolean balanco(ClassNode node) {
        MethodNode m = method(node, "(F)V", "renderItemInFirstPerson", "func_78440_a");
        if (m == null) {
            System.err.println("[Pulsar] Animações: renderItemInFirstPerson não achado:" + listMethods(node));
            return false;
        }
        // O balanço do braço é guardado numa variável logo depois de getSwingProgress(partialTicks).
        int swing = -1;
        for (AbstractInsnNode in = m.instructions.getFirst(); in != null && swing < 0; in = in.getNext()) {
            if (in instanceof MethodInsnNode && ((MethodInsnNode) in).desc.equals("(F)F")
                    && is(((MethodInsnNode) in).name, "getSwingProgress", "func_70678_g")
                    && next(in) instanceof VarInsnNode && next(in).getOpcode() == Opcodes.FSTORE) {
                swing = ((VarInsnNode) next(in)).var;
            }
        }
        if (swing < 0) {
            System.err.println("[Pulsar] Animações: balanço do braço não achado em " + m.name);
            return false;
        }
        // Enquanto usa o item, o jogo passa 0 como balanço para transformFirstPersonItem(f, 0.0F).
        int count = 0;
        for (AbstractInsnNode in = m.instructions.getFirst(); in != null; in = in.getNext()) {
            if (in.getOpcode() != Opcodes.FCONST_0) continue;
            AbstractInsnNode call = next(in);
            if (!(call instanceof MethodInsnNode)) continue;
            MethodInsnNode mi = (MethodInsnNode) call;
            if (!mi.owner.equals(node.name) || !mi.desc.equals("(FF)V") || !is(mi.name, "transformFirstPersonItem", "func_178096_b")) continue;
            InsnList list = new InsnList();
            list.add(new VarInsnNode(Opcodes.FLOAD, swing));
            list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, G, "balancoUso", "(F)F", false));
            m.instructions.insertBefore(in, list);
            m.instructions.remove(in);
            in = call;
            count++;
        }
        if (count == 0) {
            System.err.println("[Pulsar] Animações: nenhum transformFirstPersonItem com balanço 0 em " + m.name);
            return false;
        }
        Ganchos.ganchoBalanco = true;
        System.out.println("[Pulsar] Animações: gancho do balanço instalado (" + m.name + ", " + count + " lugares)");
        return true;
    }

    // ---------- cor do dano ----------

    private static boolean corDano(ClassNode node) {
        MethodNode m = method(node, "(Lnet/minecraft/entity/EntityLivingBase;FZ)Z", "setBrightness", "func_177092_a");
        if (m == null) {
            System.err.println("[Pulsar] Impacto: setBrightness não achado:" + listMethods(node));
            return false;
        }
        for (AbstractInsnNode in = m.instructions.getFirst(); in != null; in = in.getNext()) {
            if (!(in instanceof MethodInsnNode)) continue;
            MethodInsnNode mi = (MethodInsnNode) in;
            if (!mi.owner.equals("java/nio/FloatBuffer") || !mi.name.equals("flip")) continue;
            // Com o buffer da cor já preenchido e ainda na pilha: troca a cor antes do flip.
            InsnList list = new InsnList();
            list.add(new InsnNode(Opcodes.DUP));
            list.add(new VarInsnNode(Opcodes.ALOAD, 1));
            list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, G, "corDano",
                    "(Ljava/nio/FloatBuffer;Lnet/minecraft/entity/EntityLivingBase;)V", false));
            m.instructions.insertBefore(in, list);
            Ganchos.ganchoCorDano = true;
            System.out.println("[Pulsar] Impacto: gancho da cor do dano instalado (" + m.name + ")");
            return true;
        }
        System.err.println("[Pulsar] Impacto: FloatBuffer.flip não achado em " + m.name);
        return false;
    }

    // ---------- utilidades ----------

    private static MethodNode method(ClassNode node, String desc, String... names) {
        for (MethodNode m : node.methods) {
            if (!m.desc.equals(desc)) continue;
            for (String n : names) if (m.name.equals(n)) return m;
        }
        return null;
    }

    /** Campo float pelo nome; senão, o n-ésimo campo float da classe (posX e posY são os dois primeiros). */
    private static String floatField(ClassNode node, int ordinal, String... names) {
        for (FieldNode f : node.fields) {
            if (!f.desc.equals("F")) continue;
            for (String n : names) if (f.name.equals(n)) return f.name;
        }
        int i = 0;
        for (FieldNode f : node.fields) {
            if (!f.desc.equals("F") || (f.access & Opcodes.ACC_STATIC) != 0) continue;
            if (i++ == ordinal) return f.name;
        }
        return null;
    }

    private static boolean is(String name, String... names) {
        for (String n : names) if (name.equals(n)) return true;
        return false;
    }

    /** Próxima instrução de verdade (pula rótulos, linhas e quadros). */
    private static AbstractInsnNode next(AbstractInsnNode in) {
        AbstractInsnNode n = in.getNext();
        while (n != null && n.getOpcode() < 0) n = n.getNext();
        return n;
    }

    private static String listMethods(ClassNode node) {
        StringBuilder sb = new StringBuilder();
        for (MethodNode x : node.methods) sb.append(' ').append(x.name).append(x.desc);
        return sb.toString();
    }
}
