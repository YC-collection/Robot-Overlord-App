package com.marginallyclever.robotOverlord.arm3;

import javax.vecmath.Vector3f;

import com.marginallyclever.robotOverlord.BoundingVolume;
import com.marginallyclever.robotOverlord.Cylinder;
import com.marginallyclever.robotOverlord.PrimitiveSolids;
import com.marginallyclever.robotOverlord.RobotOverlord;
import com.marginallyclever.robotOverlord.RobotWithConnection;
import com.marginallyclever.robotOverlord.communications.AbstractConnection;

import com.jogamp.opengl.GL2;
import javax.swing.JPanel;

import java.util.ArrayList;


public class Arm3 
extends RobotWithConnection {
	/**
	 * serial version id
	 */
	private static final long serialVersionUID = -5065086783049069206L;

	// Collision volumes
	protected Cylinder [] volumes = new Cylinder[6];
	
	// motion now and in the future for looking-ahead
	protected Arm3MotionState motionNow;
	protected Arm3MotionState motionFuture;
	protected Arm3Dimensions armSettings;
	
	// keyboard history
	protected float aDir = 0.0f;
	protected float bDir = 0.0f;
	protected float cDir = 0.0f;

	protected float xDir = 0.0f;
	protected float yDir = 0.0f;
	protected float zDir = 0.0f;
	
	protected double speed=2;
	boolean followMode = false;
	boolean armHasMoved = false;
	
	protected boolean isPortConfirmed=false;
	
	protected Arm3ControlPanel arm3Panel=null;
	protected boolean draw_simple=false;
	
	
	public Arm3() {
		super();
		setupDimensions(new Arm3Dimensions());
	}
	
	public Arm3(Arm3Dimensions arg0) {
		super();
		setupDimensions(arg0);
	}
	
	protected void setupDimensions(Arm3Dimensions arg0) {
		armSettings = arg0;

		motionNow = new Arm3MotionState(arg0);
		motionFuture = new Arm3MotionState(arg0);
		
		setDisplayName(armSettings.getName());
		
		// set up bounding volumes
		for(int i=0;i<volumes.length;++i) {
			volumes[i] = new Cylinder();
		}
		volumes[0].setRadius(3.2f);
		volumes[1].setRadius(3.0f*0.575f);
		volumes[2].setRadius(2.2f);
		volumes[3].setRadius(1.15f);
		volumes[4].setRadius(1.2f);
		volumes[5].setRadius(1.0f*0.575f);
		
		RotateBase(0,0);
		motionNow.IK();
		motionFuture.IK();
	}
	

	public Vector3f getHome() {
		return armSettings.getHomePosition();
	}
	
	
	private void enableFK() {		
		xDir=0;
		yDir=0;
		zDir=0;
	}
	
	private void disableFK() {	
		aDir=0;
		bDir=0;
		cDir=0;
	}
	
	public void moveA(float dir) {
		aDir=dir;
		enableFK();
	}

	public void moveB(float dir) {
		bDir=dir;
		enableFK();
	}

	public void moveC(float dir) {
		cDir=dir;
		enableFK();
	}

	public void moveX(float dir) {
		xDir=dir;
		disableFK();
	}

	public void moveY(float dir) {
		yDir=dir;
		disableFK();
	}

	public void moveZ(float dir) {
		zDir=dir;
		disableFK();
	}

	
	protected void updateIK(float delta) {
		boolean changed=false;
		motionFuture.fingerPosition.set(motionNow.fingerPosition);
		float dp = (float)speed * delta;
		
		if (xDir!=0) {
			motionFuture.fingerPosition.x += xDir * dp;
			changed=true;
			xDir=0;
		}		
		if (yDir!=0) {
			motionFuture.fingerPosition.y += yDir * dp;
			changed=true;
			yDir=0;
		}
		if (zDir!=0) {
			motionFuture.fingerPosition.z += zDir * dp;
			changed=true;
			zDir=0;
		}

		if(changed) {
			if(motionFuture.movePermitted()) {
				if(!motionNow.fingerPosition.epsilonEquals(motionFuture.fingerPosition,0.1f)) {
					armHasMoved=true;
					updateGUI();
				}
			} else {
				motionFuture.set(motionNow);
			}
		}
	}
	
	
	protected void updateFK(float delta) {
		boolean changed=false;

		if (cDir!=0) {
			motionFuture.angleElbow += speed * cDir;
			changed=true;
			cDir=0;
		}
		
		if(bDir!=0) {
			motionFuture.angleShoulder += speed * bDir;
			changed=true;
			bDir=0;
		}
		
		if(aDir!=0) {
			motionFuture.angleBase += speed * aDir;
			changed=true;
			aDir=0;
		}

		if(changed) {
			if(motionFuture.CheckAngleLimits()) {
				motionFuture.FK();
				armHasMoved=true;
				updateGUI();
			} else {
				motionFuture.set(motionNow);
			}
		}
	}

	@Override
	public void prepareMove(float delta) {
		updateIK(delta);
		updateFK(delta);
	}
	

	@Override
	public void finalizeMove() {
		// copy motionFuture to motionNow
		motionNow.set(motionFuture);
		
		if( isReadyToReceive && armHasMoved ) {
			String command = armSettings.reportMove(motionFuture);
			if(command.trim().length()>0) {
				sendLineToRobot(command);
			}

			armHasMoved=false;
		}
	}

	
	@Override
	public ArrayList<JPanel> getControlPanels(RobotOverlord gui) {
		ArrayList<JPanel> list = super.getControlPanels(gui);
		
		if(list==null) list = new ArrayList<JPanel>();
		
		arm3Panel = createArm3ControlPanel(gui);
		list.add(arm3Panel);
		updateGUI();
/*
		ArrayList<JPanel> toolList = tool.getControlPanels();
		Iterator<JPanel> iter = toolList.iterator();
		while(iter.hasNext()) {
			list.add(iter.next());
		}
		*/
		return list;
	}
	
	
	protected Arm3ControlPanel createArm3ControlPanel(RobotOverlord gui) {
		return new Arm3ControlPanel(gui,this);
	}
	
	public void updateGUI() {
		Vector3f v = new Vector3f();
		v.set(motionNow.fingerPosition);
		// TODO rotate fingerPosition before adding position
		v.add(getPosition());
		arm3Panel.xPos.setText(Float.toString(roundOff(v.x)));
		arm3Panel.yPos.setText(Float.toString(roundOff(v.y)));
		arm3Panel.zPos.setText(Float.toString(roundOff(v.z)));

		arm3Panel.labelFK1.setText(Float.toString(roundOff(motionNow.angleBase)));
		arm3Panel.labelFK2.setText(Float.toString(roundOff(motionNow.angleShoulder)));
		arm3Panel.labelFK3.setText(Float.toString(roundOff(motionNow.angleElbow)));
		
		arm3Panel.labelIK1.setText(Float.toString(roundOff(motionNow.angleBase)));
		arm3Panel.labelIK2.setText(Float.toString(roundOff(motionNow.angleShoulder)));
		arm3Panel.labelIK3.setText(Float.toString(roundOff(motionNow.angleElbow)));

		//if( tool != null ) tool.updateGUI();
	}
	
	
	protected float roundOff(float v) {
		float SCALE = 1000.0f;
		
		return Math.round(v*SCALE)/SCALE;
	}
	
	
	public void render(GL2 gl2) {
		gl2.glPushMatrix();
		Vector3f p = this.getPosition();
		gl2.glTranslatef(p.x,p.y,p.z);
/*
		gl2.glTranslatef(motionNow.base.x, motionNow.base.y, motionNow.base.z);
		gl2.glRotatef(motionNow.base_pan, motionNow.base_up.x,motionNow.base_up.y,motionNow.base_up.z);
 		// for debugging difference between FK and IK
		gl2.glDisable(GL2.GL_LIGHTING);
		gl2.glColor3d(1,1,1);
		PrimitiveSolids.drawStar(gl2,motionNow.finger_tip,5);
		PrimitiveSolids.drawStar(gl2,motionNow.elbow,10);
		PrimitiveSolids.drawStar(gl2,motionNow.shoulder,15);
		gl2.glEnable(GL2.GL_LIGHTING);
*/
		//drawBounds(gl2);
		
		// these two should always match!
		drawFK(gl2);
		//drawIK(gl2);
		
		gl2.glPopMatrix();
	}
	
	
	protected void drawIK(GL2 gl2) {
		gl2.glPushMatrix();
		gl2.glTranslatef(motionNow.base.x, motionNow.base.y, motionNow.base.z);
		gl2.glRotatef(motionNow.base_pan, motionNow.base_up.x,motionNow.base_up.y,motionNow.base_up.z);
		
		gl2.glDisable(GL2.GL_DEPTH_TEST);
		gl2.glDisable(GL2.GL_LIGHTING);
		
	    Vector3f base_u = new Vector3f(1,0,0);
	    Vector3f base_v = new Vector3f(0,1,0);
	    Vector3f base_w = new Vector3f(0,0,1);
	    drawMatrix(gl2,motionNow.base,base_u,base_v,base_w);

	    Vector3f arm_plane = new Vector3f(motionNow.wrist.x,motionNow.wrist.y,0);
	    arm_plane.normalize();
		Vector3f arm_plane_normal = new Vector3f();
		Vector3f arm_up = new Vector3f(0,0,1);
		arm_plane_normal.cross(arm_plane,arm_up);

		Vector3f shoulder_v = new Vector3f(arm_plane_normal);
		shoulder_v.scale(-1);
		Vector3f shoulder_w = new Vector3f(motionNow.elbow);
		shoulder_w.sub(motionNow.shoulder);
		shoulder_w.normalize();
		Vector3f shoulder_u = new Vector3f();
		shoulder_u.cross(shoulder_v,shoulder_w);
	    drawMatrix(gl2,motionNow.shoulder,shoulder_u,shoulder_v,shoulder_w);
		
		Vector3f elbow_v = new Vector3f(arm_plane_normal);
		elbow_v.scale(-1);
		Vector3f elbow_w = new Vector3f(motionNow.wrist);
		elbow_w.sub(motionNow.elbow);
		elbow_w.normalize();
		Vector3f elbow_u = new Vector3f();
		elbow_u.cross(elbow_v,elbow_w);
	    drawMatrix(gl2,motionNow.elbow,elbow_u,elbow_v,elbow_w);

	    
		Vector3f ulna_w = new Vector3f(motionNow.wrist);
		ulna_w.sub(motionNow.elbow);
		ulna_w.normalize();
		Vector3f ulna_p = new Vector3f(motionNow.wrist);
		ulna_p.add(motionNow.elbow);
		ulna_p.scale(0.5f);

		Vector3f ulna_v = new Vector3f(arm_plane_normal);
		ulna_v.scale(-1);
		Vector3f ulna_u = new Vector3f();
		ulna_u.cross(ulna_v,ulna_w);
	    drawMatrix(gl2,ulna_p,ulna_u,ulna_v,ulna_w);

	    Vector3f wrist_u = new Vector3f(ulna_u);
	    Vector3f wrist_v = new Vector3f(ulna_v);
	    Vector3f wrist_w = new Vector3f(ulna_w);
	    drawMatrix(gl2,motionNow.wrist,wrist_u,wrist_v,wrist_w);

	    Vector3f finger_up = new Vector3f();
	    finger_up.cross(motionNow.fingerForward,motionNow.fingerRight);
	    drawMatrix(gl2,motionNow.fingerPosition,motionNow.fingerForward,motionNow.fingerRight,finger_up);

		gl2.glDisable(GL2.GL_DEPTH_TEST);
		gl2.glBegin(GL2.GL_LINES);

		gl2.glColor3f(1,1,1);
		gl2.glVertex3f(0,0,0);
		gl2.glVertex3f(motionNow.shoulder.x,motionNow.shoulder.y,motionNow.shoulder.z);

		gl2.glVertex3f(motionNow.shoulder.x,motionNow.shoulder.y,motionNow.shoulder.z);
		gl2.glVertex3f(motionNow.elbow.x,motionNow.elbow.y,motionNow.elbow.z);

		gl2.glVertex3f(motionNow.elbow.x,motionNow.elbow.y,motionNow.elbow.z);
		gl2.glVertex3f(motionNow.wrist.x,motionNow.wrist.y,motionNow.wrist.z);

		gl2.glVertex3f(motionNow.wrist.x,motionNow.wrist.y,motionNow.wrist.z);
		gl2.glVertex3f(motionNow.fingerPosition.x,motionNow.fingerPosition.y,motionNow.fingerPosition.z);

		// DEBUG START
		Vector3f ulna_forward = new Vector3f(motionNow.elbow);
		ulna_forward.sub(motionNow.wrist);
		ulna_forward.normalize();
		Vector3f ffn = new Vector3f(ulna_forward);

		Vector3f right_unrotated;
		Vector3f forward = armSettings.getHomeForward();
		Vector3f right = armSettings.getHomeRight();
		Vector3f up = new Vector3f();
		
		up.cross(forward,right);
		
		//Vector3f of = new Vector3f(forward);
		Vector3f or = new Vector3f(right);
		Vector3f ou = new Vector3f(up);
		
		//result = RotateAroundAxis(right,of,motionNow.iku);
		right_unrotated = rotateAroundAxis(right,or,motionNow.ikv);
		right_unrotated = rotateAroundAxis(right_unrotated,ou,motionNow.ikw);

		Vector3f ulna_normal = new Vector3f();
		ulna_normal.cross(motionNow.fingerForward,right_unrotated);
		ulna_normal.normalize();
		
		Vector3f ulna_up = new Vector3f();
		ulna_up.cross(ulna_forward,ulna_normal);

		ffn.normalize();
		float df= motionNow.fingerForward.dot(ffn);
		if(Math.abs(df)<0.999999) {
			Vector3f ulna_up_unrotated = new Vector3f();
			ulna_up_unrotated.cross(ulna_forward,arm_plane_normal);
			
			Vector3f finger_on_ulna = new Vector3f(motionNow.fingerForward);
			Vector3f temp = new Vector3f(ffn);
			temp.scale(df);
			finger_on_ulna.sub(temp);
			finger_on_ulna.normalize();
			
			gl2.glColor3f(0,1,1);
			gl2.glVertex3f(motionNow.wrist.x,motionNow.wrist.y,motionNow.wrist.z);
			gl2.glVertex3f(motionNow.wrist.x+ulna_up.x*3,
							motionNow.wrist.y+ulna_up.y*3,
							motionNow.wrist.z+ulna_up.z*3);
			gl2.glColor3f(1,0,1);
			gl2.glVertex3f(motionNow.wrist.x,motionNow.wrist.y,motionNow.wrist.z);
			gl2.glVertex3f(motionNow.wrist.x+ulna_forward.x*-3,
							motionNow.wrist.y+ulna_forward.y*-3,
							motionNow.wrist.z+ulna_forward.z*-3);
			gl2.glColor3f(1,1,1);
			gl2.glVertex3f(motionNow.wrist.x,motionNow.wrist.y,motionNow.wrist.z);
			gl2.glVertex3f(motionNow.wrist.x+finger_on_ulna.x,
							motionNow.wrist.y+finger_on_ulna.y,
							motionNow.wrist.z+finger_on_ulna.z);

		}
		// DEBUG END
		
		gl2.glEnd();


		gl2.glEnable(GL2.GL_DEPTH_TEST);
		gl2.glEnable(GL2.GL_LIGHTING);
		
		gl2.glPopMatrix();
	}

	
	protected void drawMatrix(GL2 gl2,Vector3f p,Vector3f u,Vector3f v,Vector3f w) {
		drawMatrix(gl2,p,u,v,w,1);
	}
	
	protected void drawMatrix(GL2 gl2,Vector3f p,Vector3f u,Vector3f v,Vector3f w,float scale) {
		gl2.glPushMatrix();
		gl2.glDisable(GL2.GL_DEPTH_TEST);
		gl2.glTranslatef(p.x, p.y, p.z);
		gl2.glScalef(scale, scale, scale);
		
		gl2.glBegin(GL2.GL_LINES);
		gl2.glColor3f(1,1,0);		gl2.glVertex3f(0,0,0);		gl2.glVertex3f(u.x,u.y,u.z);
		gl2.glColor3f(0,1,1);		gl2.glVertex3f(0,0,0);		gl2.glVertex3f(v.x,v.y,v.z);
		gl2.glColor3f(1,0,1);		gl2.glVertex3f(0,0,0);		gl2.glVertex3f(w.x,w.y,w.z);
		gl2.glEnd();
		
		gl2.glEnable(GL2.GL_DEPTH_TEST);
		gl2.glPopMatrix();
	}
	
	protected void drawFK(GL2 gl2) {
		Vector3f a0 = new Vector3f(armSettings.getBaseToShoulderX(),0,armSettings.getBaseToShoulderZ());
		Vector3f a1 = new Vector3f(0,0,armSettings.getShoulderToElbow());
		Vector3f a2 = new Vector3f(0,0,armSettings.getElbowToWrist());
		Vector3f a3 = new Vector3f(0,0,armSettings.getWristToFinger());

		// base 
		gl2.glPushMatrix();
		gl2.glTranslatef(motionNow.base.x, motionNow.base.y, motionNow.base.z);
		gl2.glRotatef(motionNow.base_pan, motionNow.base_up.x,motionNow.base_up.y,motionNow.base_up.z);
		
		gl2.glColor3f(1,1,1);
		gl2.glRotatef(motionNow.angleBase,0,0,1);
		gl2.glColor3f(0,0,1);
		PrimitiveSolids.drawBox(gl2,4,armSettings.getBaseToShoulderX()*2,armSettings.getBaseToShoulderZ());

		// shoulder
		gl2.glTranslatef(a0.x,a0.y,a0.z);
		gl2.glRotatef(90+motionNow.angleShoulder,0,1,0);
		gl2.glColor3f(0,1,0);
		PrimitiveSolids.drawCylinder(gl2,3.2f,3.2f);
		
		// bicep
		gl2.glColor3f(0,0,1);
		//PrimitiveSolids.drawBox(gl2,3,3,SHOULDER_TO_ELBOW);
		gl2.glPushMatrix();
		gl2.glTranslatef(a1.x/2,a1.y/2,a1.z/2);
		gl2.glRotatef(90,1,0,0);
		PrimitiveSolids.drawCylinder(gl2, armSettings.getShoulderToElbow()/2.0f, 3.0f*0.575f);
		gl2.glPopMatrix();

		// elbow
		gl2.glTranslatef(a1.x,a1.y,a1.z);
		gl2.glRotatef(180-motionNow.angleElbow-motionNow.angleShoulder,0,1,0);
		gl2.glColor3f(0,1,0);
		PrimitiveSolids.drawCylinder(gl2,2.2f,2.2f);
		gl2.glColor3f(0,0,1);
		//PrimitiveSolids.drawBox(gl2,2,2,ELBOW_TO_WRIST);

		// ulna
		gl2.glPushMatrix();
		gl2.glTranslatef(a2.x/2,a2.y/2,a2.z/2);
		gl2.glRotatef(90,1,0,0);
		PrimitiveSolids.drawCylinder(gl2, armSettings.getElbowToWrist()/2.0f, 1.15f);
		gl2.glPopMatrix();

		// wrist
		gl2.glTranslatef(a2.x,a2.y,a2.z);
		gl2.glRotatef(-180+motionNow.angleElbow,0,1,0);
		gl2.glColor3f(0,1,0);
		PrimitiveSolids.drawCylinder(gl2,1.2f,1.2f);
		gl2.glColor3f(0,0,1);
		//PrimitiveSolids.drawBox(gl2,1,1,WRIST_TO_FINGER);

		// finger tip
		gl2.glPushMatrix();
		gl2.glTranslatef(a3.x/2,a3.y/3,a3.z/2);
		gl2.glRotatef(90,1,0,0);
		PrimitiveSolids.drawCylinder(gl2, armSettings.getWristToFinger()/2.0f, 1.0f*0.575f);
		gl2.glPopMatrix();

		gl2.glTranslatef(a3.x,a3.y,a3.z);	
		//gl2.glRotatef(-motionNow.angle_3,a2.x,a2.y,a2.z);
		//gl2.glRotatef(motionNow.angle_4,0,1,0);
		//gl2.glRotatef(-180+motionNow.angle_2,0,1,0);
		gl2.glRotatef(-90,0,1,0);
		// draw finger tip orientation
		gl2.glDisable(GL2.GL_LIGHTING);
		gl2.glBegin(GL2.GL_LINES);
		gl2.glColor3f(1,0,1);  // magenta
		gl2.glVertex3f(0,0,0);
		Vector3f hf=armSettings.getHomeForward();
		Vector3f hr=armSettings.getHomeRight();
		gl2.glVertex3f(hr.x,hr.y,hr.z);
		gl2.glColor3f(0,1,0);  // green
		gl2.glVertex3f(0,0,0);
		gl2.glVertex3f(hf.x,hf.y,hf.z);
		gl2.glEnd();
		gl2.glEnable(GL2.GL_LIGHTING);
		// TODO draw tool here
		gl2.glPopMatrix();

		if(draw_simple) {
			gl2.glDisable(GL2.GL_DEPTH_TEST);
			gl2.glDisable(GL2.GL_LIGHTING);

			// base 
			gl2.glPushMatrix();
			gl2.glTranslatef(motionNow.base.x, motionNow.base.y, motionNow.base.z);
			gl2.glRotatef(motionNow.base_pan, motionNow.base_up.x,motionNow.base_up.y,motionNow.base_up.z);
			
			gl2.glColor3f(1,1,1);
			gl2.glRotatef(motionNow.angleBase,0,0,1);

			gl2.glBegin(GL2.GL_LINES);
			gl2.glVertex3f(0,0,0);
			gl2.glVertex3f(a0.x,a0.y,a0.z);
			gl2.glEnd();

			// shoulder
			gl2.glTranslatef(a0.x,a0.y,a0.z);
			gl2.glRotatef(90+motionNow.angleShoulder,0,1,0);
			
			// bicep
			gl2.glColor3f(0,0,1);
			gl2.glBegin(GL2.GL_LINES);
			gl2.glVertex3f(0,0,0);
			gl2.glVertex3f(a1.x,a1.y,a1.z);
			gl2.glEnd();
	
			// elbow
			gl2.glTranslatef(a1.x,a1.y,a1.z);
			gl2.glRotatef(180-motionNow.angleElbow-motionNow.angleShoulder,0,1,0);
	
			// ulna
			gl2.glBegin(GL2.GL_LINES);
			gl2.glVertex3f(0,0,0);
			gl2.glVertex3f(a2.x,a2.y,a2.z);
			gl2.glEnd();
	
			// wrist
			gl2.glTranslatef(a2.x,a2.y,a2.z);
			gl2.glRotatef(-180+motionNow.angleElbow,0,1,0);
			
			// finger tip
			gl2.glBegin(GL2.GL_LINES);
			gl2.glVertex3f(0,0,0);
			gl2.glVertex3f(a3.x,a3.y,a3.z);
			gl2.glEnd();
			
			gl2.glTranslatef(a3.x,a3.y,a3.z);	

			// draw finger tip orientation			
			gl2.glRotatef(-90,0,1,0);
			gl2.glBegin(GL2.GL_LINES);
			gl2.glColor3f(1,0,1);  // magenta
			gl2.glVertex3f(0,0,0);
			gl2.glVertex3f(hr.x*5,hr.y*5,hr.z*5);
			gl2.glColor3f(0,1,0);  // green
			gl2.glVertex3f(0,0,0);
			gl2.glVertex3f(hf.x,hf.y,hf.z);
			gl2.glEnd();
			
			// TODO draw tool here
			
			gl2.glPopMatrix();

			gl2.glEnable(GL2.GL_LIGHTING);
			gl2.glEnable(GL2.GL_DEPTH_TEST);
		}
	}

	
	protected void drawBounds(GL2 gl2) {
		// base
		
		gl2.glPushMatrix();
		gl2.glTranslatef(motionNow.base.x, motionNow.base.y, motionNow.base.z);
		gl2.glRotatef(motionNow.angleBase,0,0,1);
		gl2.glColor3f(0,0,1);
		PrimitiveSolids.drawBox(gl2,4,armSettings.getBaseToShoulderX()*2,armSettings.getBaseToShoulderZ());
		gl2.glPopMatrix();
		
		//gl2.glDisable(GL2.GL_LIGHTING);

		gl2.glColor3f(0,1,0);	PrimitiveSolids.drawCylinder(gl2,volumes[0]);  // shoulder
		gl2.glColor3f(0,0,1);	PrimitiveSolids.drawCylinder(gl2,volumes[1]);  // bicep
		gl2.glColor3f(0,1,0);	PrimitiveSolids.drawCylinder(gl2,volumes[2]);  // elbow
		gl2.glColor3f(0,0,1);	PrimitiveSolids.drawCylinder(gl2,volumes[3]);  // ulna
		gl2.glColor3f(0,1,0);	PrimitiveSolids.drawCylinder(gl2,volumes[4]);  // wrist
		gl2.glColor3f(0,0,1);	PrimitiveSolids.drawCylinder(gl2,volumes[5]);  // elbow

		//gl2.glEnable(GL2.GL_LIGHTING);
		
		// TODO draw tool here
	}
	
	
	public boolean isPortConfirmed() {
		return isPortConfirmed;
	}
	

	public void MoveBase(Vector3f dp) {
		motionFuture.base.set(dp);
	}
	
	
	public void RotateBase(float pan,float tilt) {
		motionFuture.base_pan=pan;
		motionFuture.base_tilt=tilt;
		
		motionFuture.base_forward.y = (float)Math.sin(pan * Math.PI/180.0) * (float)Math.cos(tilt * Math.PI/180.0);
		motionFuture.base_forward.x = (float)Math.cos(pan * Math.PI/180.0) * (float)Math.cos(tilt * Math.PI/180.0);
		motionFuture.base_forward.z =                                        (float)Math.sin(tilt * Math.PI/180.0);
		motionFuture.base_forward.normalize();
		
		motionFuture.base_up.set(0,0,1);
	
		motionFuture.base_right.cross(motionFuture.base_forward, motionFuture.base_up);
		motionFuture.base_right.normalize();
		motionFuture.base_up.cross(motionFuture.base_right, motionFuture.base_forward);
		motionFuture.base_up.normalize();
	}
	
	
	public BoundingVolume [] GetBoundingVolumes() {
		// shoulder joint
		Vector3f t1=new Vector3f(motionFuture.base_right);
		t1.scale(volumes[0].getRadius()/2);
		t1.add(motionFuture.shoulder);
		Vector3f t2=new Vector3f(motionFuture.base_right);
		t2.scale(-volumes[0].getRadius()/2);
		t2.add(motionFuture.shoulder);
		volumes[0].SetP1(GetWorldCoordinatesFor(t1));
		volumes[0].SetP2(GetWorldCoordinatesFor(t2));
		// bicep
		volumes[1].SetP1(GetWorldCoordinatesFor(motionFuture.shoulder));
		volumes[1].SetP2(GetWorldCoordinatesFor(motionFuture.elbow));
		// elbow
		t1.set(motionFuture.base_right);
		t1.scale(volumes[0].getRadius()/2);
		t1.add(motionFuture.elbow);
		t2.set(motionFuture.base_right);
		t2.scale(-volumes[0].getRadius()/2);
		t2.add(motionFuture.elbow);
		volumes[2].SetP1(GetWorldCoordinatesFor(t1));
		volumes[2].SetP2(GetWorldCoordinatesFor(t2));
		// ulna
		volumes[3].SetP1(GetWorldCoordinatesFor(motionFuture.elbow));
		volumes[3].SetP2(GetWorldCoordinatesFor(motionFuture.wrist));
		// wrist
		t1.set(motionFuture.base_right);
		t1.scale(volumes[0].getRadius()/2);
		t1.add(motionFuture.wrist);
		t2.set(motionFuture.base_right);
		t2.scale(-volumes[0].getRadius()/2);
		t2.add(motionFuture.wrist);
		volumes[4].SetP1(GetWorldCoordinatesFor(t1));
		volumes[4].SetP2(GetWorldCoordinatesFor(t2));
		// finger
		volumes[5].SetP1(GetWorldCoordinatesFor(motionFuture.wrist));
		volumes[5].SetP2(GetWorldCoordinatesFor(motionFuture.fingerPosition));
		
		return volumes;
	}
	
	
	Vector3f GetWorldCoordinatesFor(Vector3f in) {
		Vector3f out = new Vector3f(motionFuture.base);
		
		Vector3f tempx = new Vector3f(motionFuture.base_forward);
		tempx.scale(in.x);
		out.add(tempx);

		Vector3f tempy = new Vector3f(motionFuture.base_right);
		tempy.scale(-in.y);
		out.add(tempy);

		Vector3f tempz = new Vector3f(motionFuture.base_up);
		tempz.scale(in.z);
		out.add(tempz);
				
		return out;
	}
	
		
	/**
	 * Rotate the point xyz around the line passing through abc with direction uvw
	 * http://inside.mines.edu/~gmurray/ArbitraryAxisRotation/ArbitraryAxisRotation.html
	 * Special case where abc=0
	 * @param vec
	 * @param axis
	 * @param angle
	 * @return
	 */
	static public Vector3f rotateAroundAxis(Vector3f vec,Vector3f axis,float angle) {
		float C = (float)Math.cos(angle);
		float S = (float)Math.sin(angle);
		float x = vec.x;
		float y = vec.y;
		float z = vec.z;
		float u = axis.x;
		float v = axis.y;
		float w = axis.z;
		
		// (a*( v*v + w*w) - u*(b*v + c*w - u*x - v*y - w*z))(1.0-C)+x*C+(-c*v + b*w - w*y + v*z)*S
		// (b*( u*u + w*w) - v*(a*v + c*w - u*x - v*y - w*z))(1.0-C)+y*C+( c*u - a*w + w*x - u*z)*S
		// (c*( u*u + v*v) - w*(a*v + b*v - u*x - v*y - w*z))(1.0-C)+z*C+(-b*u + a*v - v*x + u*y)*S
		// but a=b=c=0 so
		// x' = ( -u*(- u*x - v*y - w*z)) * (1.0-C) + x*C + ( - w*y + v*z)*S
		// y' = ( -v*(- u*x - v*y - w*z)) * (1.0-C) + y*C + ( + w*x - u*z)*S
		// z' = ( -w*(- u*x - v*y - w*z)) * (1.0-C) + z*C + ( - v*x + u*y)*S
		
		float a = (-u*x - v*y - w*z);

		return new Vector3f( (-u*a) * (1.0f-C) + x*C + ( -w*y + v*z)*S,
							 (-v*a) * (1.0f-C) + y*C + (  w*x - u*z)*S,
							 (-w*a) * (1.0f-C) + z*C + ( -v*x + u*y)*S);
	}

	
	@Override
	public void dataAvailable(AbstractConnection arg0,String data) {
		super.dataAvailable(arg0, data);
		
		if(data.contains(armSettings.getHello())) {
			isPortConfirmed=true;
			
			// we are not homed and we have not begun to home
			if(armSettings.getHomeAutomaticallyOnStartup()) {
				// this should be sent by a human when they are ready
				sendLineToRobot("G28");
				motionFuture.fingerPosition.set(armSettings.getHomePosition());  // HOME_* should match values in robot firmware.
				motionFuture.IK();
				sendLineToRobot("G92 X"+motionFuture.fingerPosition.x
								+" Y"+motionFuture.fingerPosition.y
								+" Z"+motionFuture.fingerPosition.z);
				followMode=true;
			}
		}
	}
	
	public double getSpeed() {
		// TODO Auto-generated method stub
		return speed;
	}
	
	public void setSpeed(double s) {
		speed=s;
	}
	
	public void doAbout() {
		armSettings.doAbout();
	}
}
