package com.marginallyclever.robotOverlord;

import javax.vecmath.Vector3f;

import com.jogamp.opengl.GL2;


public class Waypoint extends Entity {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3308765172620128567L;


	public Waypoint() {}

		
	@Override
	public String getDisplayName() {
		return "Waypoint";
	}
	
	
	public void render(GL2 gl2) {
		gl2.glPushMatrix();
			Vector3f p = this.getPosition();
			final float size=2;
			gl2.glTranslatef(p.x, p.y, p.z);
			// draw placeholder
			gl2.glColor3f(1, 0, 0);
			PrimitiveSolids.drawBox(gl2,  size, 0.1f, 0.1f);
			gl2.glColor3f(0, 1, 0);
			PrimitiveSolids.drawBox(gl2,   0.1f,size, 0.1f);
			gl2.glColor3f(0, 0, 1);
			gl2.glPushMatrix();
			gl2.glTranslatef(0, 0, -size/2);
			PrimitiveSolids.drawBox(gl2,   0.1f, 0.1f,size);
			gl2.glPopMatrix();
		gl2.glPopMatrix();
	}
}
