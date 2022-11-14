package com.klix.backend.model.uniinterface;

public class Institution {
	
	int id;
	int[] members;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int[] getMembers() {
		return members;
	}
	public void setMembers(int[] members) {
		this.members = members;
	}
}
