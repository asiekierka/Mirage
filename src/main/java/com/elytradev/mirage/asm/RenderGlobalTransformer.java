/*
 * The MIT License
 *
 * Copyright (c) 2017 Elucent, William Thompson (unascribed), and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.elytradev.mirage.asm;

import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import com.elytradev.mini.MiniTransformer;
import com.elytradev.mini.PatchContext;
import com.elytradev.mini.annotation.Patch;

@Patch.Class("net.minecraft.client.renderer.RenderGlobal")
public class RenderGlobalTransformer extends MiniTransformer {
	
	@Patch.Method(
			srg="func_174982_a",
			mcp="renderBlockLayer",
			descriptor="(Lnet/minecraft/util/BlockRenderLayer;)V"
		)
	public void patchRenderBlockLayer(PatchContext ctx) {
		ctx.jumpToStart();
		ctx.add(new MethodInsnNode(INVOKESTATIC, "com/elytradev/mirage/asm/Hooks", "enableLightShader", "()V", false));
		ctx.jumpToEnd();
		ctx.searchBackward(new InsnNode(RETURN)).jumpBefore();
		ctx.add(new MethodInsnNode(INVOKESTATIC, "com/elytradev/mirage/asm/Hooks", "disableLightShader", "()V", false));
	}
	
}
