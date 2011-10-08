package me.yonatan.g2g.core.model;

import java.io.Serializable;

import lombok.Data;
import lombok.NonNull;

@SuppressWarnings("serial")
@Data
public class MessageData implements Serializable {

	@NonNull
	private String gid;

	@NonNull
	private long uid;

	@NonNull
	private int id;
}
