package com.thalmic.android.sample.helloworld;

import com.thalmic.myo.Myo;

public class MyMyo implements Comparable<MyMyo> {
	private Myo myo;
	private float rotationX;
	private float rotationY;
	private float rotationZ;
	private float ref_rotationZ;
	private float ref_rotationX;
	private float ref_rotationY;
	private float sum = 0;
	private int pos_x;
	private int pos_y;
	private int[] color;

	public int getPos_x() {
		return pos_x;
	}

	public void setPos_x(int pos_x) {
		this.pos_x = pos_x;
	}

	public int getPos_y() {
		return pos_y;
	}

	public void setPos_y(int pos_y) {
		this.pos_y = pos_y;
	}

	public MyMyo(Myo myo) {
		super();
		this.myo = myo;
	}

	public MyMyo(Myo myo, float rotationX, float rotationY, float rotationZ) {
		super();
		this.myo = myo;
		this.rotationX = rotationX;
		this.rotationY = rotationY;
		this.rotationZ = rotationZ;
	}

	public MyMyo(Myo myo, float rotationX, float rotationY, float rotationZ,
			float ref_rotationZ, float ref_rotationX, float ref_rotationY) {
		super();
		this.myo = myo;
		this.rotationX = rotationX;
		this.rotationY = rotationY;
		this.rotationZ = rotationZ;
		this.ref_rotationZ = ref_rotationZ;
		this.ref_rotationX = ref_rotationX;
		this.ref_rotationY = ref_rotationY;
	}

	public Myo getMyo() {
		return myo;
	}

	public void setMyo(Myo myo) {
		this.myo = myo;
	}

	public float getRotationX() {
		return rotationX;
	}

	public void setRotationX(float rotationX) {
		this.rotationX = rotationX;
	}

	public float getRotationY() {
		return rotationY;
	}

	public void setRotationY(float rotationY) {
		this.rotationY = rotationY;
	}

	public float getRotationZ() {
		return rotationZ;
	}

	public void setRotationZ(float rotationZ) {
		this.rotationZ = rotationZ;
	}

	public float getRef_rotationZ() {
		return ref_rotationZ;
	}

	public void setRef_rotationZ(float ref_rotationZ) {
		this.ref_rotationZ = ref_rotationZ;
	}

	public float getRef_rotationX() {
		return ref_rotationX;
	}

	public void setRef_rotationX(float ref_rotationX) {
		this.ref_rotationX = ref_rotationX;
	}

	public float getRef_rotationY() {
		return ref_rotationY;
	}

	public void setRef_rotationY(float ref_rotationY) {
		this.ref_rotationY = ref_rotationY;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MyMyo other = (MyMyo) obj;
		if (myo == null) {
			if (other.myo != null)
				return false;
		} else if (!myo.equals(other.myo))
			return false;
		return true;
	}

	public void posToRef() {
		ref_rotationX = rotationX;
		ref_rotationY = rotationY;
		ref_rotationZ = rotationZ;
	}

	public void addSum() {
		sum += countDistance();
		ref_rotationX = rotationX;
		ref_rotationY = rotationY;
		ref_rotationZ = rotationZ;
	}

	public void clearSum() {
		sum = 0;
	}

	public float getSum() {
		return sum;
	}

	private float countDistance() {
		return Math.abs(ref_rotationX - rotationX)
				+ Math.abs(ref_rotationY - rotationY)
				+ Math.abs(ref_rotationZ - rotationZ);
	}

	@Override
	public int compareTo(MyMyo another) {
		// TODO Auto-generated method stub
		return (int) (another.getSum() - getSum());
	}

	public void setColor(int i, int j, int k) {
		color = new int[] { i, j, k };
	}

	public int[] getColor() {
		return color;
	}
}
