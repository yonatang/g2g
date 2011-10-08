package me.yonatan.g2g.core.model;

import javax.enterprise.inject.Model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@ToString(exclude = "password")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@Setter(value = AccessLevel.PROTECTED)
@Model
public class GmailCredentials {

	@NonNull
	private String email;

	@NonNull
	private String password;

}
