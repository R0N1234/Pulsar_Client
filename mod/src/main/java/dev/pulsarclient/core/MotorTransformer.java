package dev.pulsarclient.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Ganchos do Motor Pulsar dentro do Minecraft. Cada um entra no começo de um método do jogo:
 * "se o Motor disser que não precisa, sai sem fazer nada".
 *
 * - RenderManager.shouldRender: entidades escondidas atrás de paredes ou longe não são desenhadas (nem sombra, nem nome).
 * - TileEntityRendererDispatcher.renderTileEntity: baús, placas, cabeças e banners longe não são desenhados.
 * - TextureMap.updateAnimations: animação de água, lava e fogo pode ser desligada.
 * - EffectRenderer.addEffect: limite de partículas novas por tick.
 *
 * Os nomes dos métodos no jogo publicado são os nomes SRG (func_...). Se algum não for encontrado,
 * o gancho simplesmente não é instalado e o log mostra os métodos que a classe tem.
 */
public class MotorTransformer implements IClassTransformer {
    private static final String HOOKS = "dev/pulsarclient/core/MotorHooks";

    private static final class Hook {
        final String className;
        final String label;
        final String[] names;
        final String desc;
        final boolean returnsBoolean;
        final int argLoad;
        final String hookName;
        final String hookDesc;

        Hook(String className, String label, String[] names, String desc, boolean returnsBoolean, int argLoad, String hookName, String hookDesc) {
            this.className = className;
            this.label = label;
            this.names = names;
            this.desc = desc;
            this.returnsBoolean = returnsBoolean;
            this.argLoad = argLoad;
            this.hookName = hookName;
            this.hookDesc = hookDesc;
        }
    }

    private static final Hook[] HOOKS_LIST = {
            new Hook("net.minecraft.client.renderer.entity.RenderManager", "entidades",
                    new String[]{"func_178635_a", "shouldRender"},
                    "(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;DDD)Z", true, 1,
                    "shouldRenderEntity", "(Lnet/minecraft/entity/Entity;)Z"),
            new Hook("net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher", "blocos especiais",
                    new String[]{"func_180546_a", "renderTileEntity"},
                    "(Lnet/minecraft/tileentity/TileEntity;FI)V", false, 1,
                    "shouldRenderTile", "(Lnet/minecraft/tileentity/TileEntity;)Z"),
            new Hook("net.minecraft.client.renderer.texture.TextureMap", "animações",
                    new String[]{"func_94248_c", "updateAnimations"}, "()V", false, -1,
                    "animate", "()Z"),
            new Hook("net.minecraft.client.particle.EffectRenderer", "partículas",
                    new String[]{"func_78873_a", "addEffect"}, "(Lnet/minecraft/client/particle/EntityFX;)V", false, -1,
                    "allowParticle", "()Z"),
    };

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || transformedName == null) return bytes;
        List<Hook> hooks = new ArrayList<Hook>();
        for (Hook h : HOOKS_LIST) if (h.className.equals(transformedName)) hooks.add(h);
        if (hooks.isEmpty()) return bytes;
        try {
            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, 0);
            boolean changed = false;
            for (Hook h : hooks) {
                MethodNode m = find(node, h);
                if (m == null) {
                    StringBuilder sb = new StringBuilder();
                    for (MethodNode x : node.methods) sb.append(' ').append(x.name).append(x.desc);
                    System.err.println("[Pulsar] Motor: gancho de " + h.label + " não instalado, método não achado em " + transformedName + ":" + sb);
                    continue;
                }
                inject(node, m, h);
                MotorFlags.installed(h.label);
                changed = true;
                System.out.println("[Pulsar] Motor: gancho de " + h.label + " instalado (" + m.name + ")");
            }
            if (!changed) return bytes;
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            return writer.toByteArray();
        } catch (Throwable t) {
            System.err.println("[Pulsar] Motor: não deu para instalar os ganchos em " + transformedName + ": " + t);
            return bytes;
        }
    }

    private static MethodNode find(ClassNode node, Hook h) {
        for (MethodNode m : node.methods) {
            if (!m.desc.equals(h.desc)) continue;
            for (String n : h.names) if (m.name.equals(n)) return m;
        }
        // Se o nome mudou mas só existe um método com essa assinatura, é ele (não vale para "()V", que é comum demais).
        if (h.desc.equals("()V")) return null;
        MethodNode only = null;
        for (MethodNode m : node.methods) {
            if (!m.desc.equals(h.desc)) continue;
            if (only != null) return null;
            only = m;
        }
        return only;
    }

    private static void inject(ClassNode node, MethodNode m, Hook h) {
        LabelNode go = new LabelNode();
        InsnList list = new InsnList();
        if (h.argLoad >= 0) list.add(new VarInsnNode(Opcodes.ALOAD, h.argLoad));
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, h.hookName, h.hookDesc, false));
        list.add(new JumpInsnNode(Opcodes.IFNE, go));
        if (h.returnsBoolean) {
            list.add(new InsnNode(Opcodes.ICONST_0));
            list.add(new InsnNode(Opcodes.IRETURN));
        } else {
            list.add(new InsnNode(Opcodes.RETURN));
        }
        list.add(go);
        // No começo do método o estado é o mesmo da entrada (só os parâmetros), então o quadro é "igual ao inicial".
        if ((node.version & 0xFFFF) >= Opcodes.V1_6) list.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        m.instructions.insert(list);
    }
}
