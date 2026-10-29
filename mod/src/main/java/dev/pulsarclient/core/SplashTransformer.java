package dev.pulsarclient.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Coloca o desenho do Pulsar dentro do laço da tela de carregamento do Forge (SplashProgress).
 *
 * O laço do Forge faz: limpa a tela, monta a projeção, desenha logo, barras e bigorna, e então mostra o quadro.
 * Logo depois da projeção entra {@code if (PulsarSplash.draw()) pula para "mostrar o quadro"}.
 * Se o desenho do Pulsar falhar, draw() devolve false e a tela normal do Forge aparece.
 */
public class SplashTransformer implements IClassTransformer {
    private static final String TARGET = "net.minecraftforge.fml.client.SplashProgress$";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || transformedName == null || !transformedName.startsWith(TARGET)) return bytes;
        try {
            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, 0);
            if (!node.interfaces.contains("java/lang/Runnable")) return bytes;
            for (MethodNode m : node.methods) {
                if (!m.name.equals("run") || !m.desc.equals("()V") || !patch(m)) continue;
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
                    @Override
                    protected String getCommonSuperClass(String a, String b) {
                        return "java/lang/Object";
                    }
                };
                node.accept(writer);
                System.out.println("[Pulsar] Tela de carregamento do Pulsar instalada em " + transformedName);
                return writer.toByteArray();
            }
        } catch (Throwable t) {
            System.err.println("[Pulsar] Não deu para trocar a tela de carregamento: " + t);
        }
        return bytes;
    }

    private static boolean patch(MethodNode m) {
        AbstractInsnNode ortho = null;
        AbstractInsnNode afterProjection = null;
        AbstractInsnNode present = null;
        for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (ortho == null) {
                if (isGl(insn, "glOrtho")) ortho = insn;
            } else if (afterProjection == null) {
                if (isGl(insn, "glLoadIdentity")) afterProjection = insn;
            } else if (insn.getOpcode() == Opcodes.GETSTATIC && ((FieldInsnNode) insn).name.equals("mutex")) {
                present = insn;
                break;
            }
        }
        if (present == null) return false;
        LabelNode skip = new LabelNode();
        m.instructions.insertBefore(present, skip);
        InsnList hook = new InsnList();
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "dev/pulsarclient/core/PulsarSplash", "draw", "()Z", false));
        hook.add(new JumpInsnNode(Opcodes.IFNE, skip));
        m.instructions.insert(afterProjection, hook);
        return true;
    }

    private static boolean isGl(AbstractInsnNode insn, String name) {
        return insn instanceof MethodInsnNode
                && ((MethodInsnNode) insn).owner.equals("org/lwjgl/opengl/GL11")
                && ((MethodInsnNode) insn).name.equals(name);
    }
}
