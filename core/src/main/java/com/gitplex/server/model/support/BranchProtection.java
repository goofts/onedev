package com.gitplex.server.model.support;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.gitplex.server.model.Account;
import com.gitplex.server.model.Depot;
import com.gitplex.server.model.Team;
import com.gitplex.server.util.PathUtils;
import com.gitplex.server.util.editable.annotation.BranchPattern;
import com.gitplex.server.util.editable.annotation.Editable;
import com.gitplex.server.util.reviewappointment.ReviewAppointment;

@Editable
public class BranchProtection implements Serializable {

	private static final long serialVersionUID = 1L;

	private String branch;
	
	private boolean noForcedPush = true;
	
	private boolean noDeletion = true;
	
	private String reviewAppointmentExpr;
	
	private transient Optional<ReviewAppointment> reviewAppointmentOpt;
	
	private List<FileProtection> fileProtections = new ArrayList<>();

	@Editable(order=100, description="Specify branch to be protected. Wildcard may be used to "
			+ "specify multiple branches")
	@BranchPattern
	@NotEmpty
	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

	@Editable(order=200, description="Check this to not allow forced push")
	public boolean isNoForcedPush() {
		return noForcedPush;
	}

	public void setNoForcedPush(boolean noForcedPush) {
		this.noForcedPush = noForcedPush;
	}

	@Editable(order=300, description="Check this to not allow branch deletion")
	public boolean isNoDeletion() {
		return noDeletion;
	}

	public void setNoDeletion(boolean noDeletion) {
		this.noDeletion = noDeletion;
	}

	@Editable(order=400, name="Reviewers", description="Optionally specify required reviewers for changes of "
			+ "specified branch. Note that the user submitting the change is considered to reviewed the change "
			+ "automatically")
	@com.gitplex.server.util.editable.annotation.ReviewAppointment
	public String getReviewAppointmentExpr() {
		return reviewAppointmentExpr;
	}

	public void setReviewAppointmentExpr(String reviewAppointmentExpr) {
		this.reviewAppointmentExpr = reviewAppointmentExpr;
	}

	@Editable(order=500, description="Optionally specify additional users to review particular paths. For each changed file, "
			+ "the first matched file protection setting will be used")
	@NotNull
	public List<FileProtection> getFileProtections() {
		return fileProtections;
	}

	public void setFileProtections(List<FileProtection> fileProtections) {
		this.fileProtections = fileProtections;
	}
	
	@Nullable
	public FileProtection getFileProtection(String file) {
		for (FileProtection protection: fileProtections) {
			if (PathUtils.matchChildAware(protection.getPath(), file))
				return protection;
		}
		return null;
	}
	
	@Nullable
	public ReviewAppointment getReviewAppointment(Depot depot) {
		if (reviewAppointmentOpt == null) {
			if (reviewAppointmentExpr != null)
				reviewAppointmentOpt = Optional.of(new ReviewAppointment(depot, reviewAppointmentExpr));
			else
				reviewAppointmentOpt = Optional.empty();
		}
		return reviewAppointmentOpt.orElse(null);
	}
	
	public void onDepotTransferred(Depot depot) {
		ReviewAppointment reviewAppointment = getReviewAppointment(depot);
		if (reviewAppointment != null) {
			reviewAppointment.getTeams().clear();
			setReviewAppointmentExpr(reviewAppointment.toExpr());
		}
		
		for (Iterator<FileProtection> it = getFileProtections().iterator(); it.hasNext();) {
			FileProtection fileProtection = it.next();
			reviewAppointment = fileProtection.getReviewAppointment(depot);
			if (reviewAppointment != null) {
				reviewAppointment.getTeams().clear();
				String reviewAppointmentExpr = reviewAppointment.toExpr();
				if (reviewAppointmentExpr != null)
					fileProtection.setReviewAppointmentExpr(reviewAppointmentExpr);
				else
					it.remove();
			} else {
				it.remove();
			}
		}
	}
	
	public void onTeamRename(Depot depot, String oldName, String newName) {
		ReviewAppointment reviewAppointment = getReviewAppointment(depot);
		if (reviewAppointment != null) {
			for (Team team: reviewAppointment.getTeams().keySet()) {
				if (team.getName().equals(oldName))
					team.setName(newName);
			}
			setReviewAppointmentExpr(reviewAppointment.toExpr());
		}
		
		for (Iterator<FileProtection> it = getFileProtections().iterator(); it.hasNext();) {
			FileProtection fileProtection = it.next();
			reviewAppointment = fileProtection.getReviewAppointment(depot);
			if (reviewAppointment != null) {
				for (Team team: reviewAppointment.getTeams().keySet()) {
					if (team.getName().equals(oldName))
						team.setName(newName);
				}
				fileProtection.setReviewAppointmentExpr(reviewAppointment.toExpr());
			} else {
				it.remove();
			}
		}
	}
	
	public void onTeamDelete(Depot depot, String teamName) {
		ReviewAppointment reviewAppointment = getReviewAppointment(depot);
		if (reviewAppointment != null) {
			for (Iterator<Map.Entry<Team, Integer>> it = reviewAppointment.getTeams().entrySet().iterator(); 
					it.hasNext();) {
				Team team = it.next().getKey();
				if (team.getName().equals(teamName))
					it.remove();
			}
			setReviewAppointmentExpr(reviewAppointment.toExpr());
		}
		
		for (Iterator<FileProtection> it = getFileProtections().iterator(); it.hasNext();) {
			FileProtection fileProtection = it.next();
			reviewAppointment = fileProtection.getReviewAppointment(depot);
			if (reviewAppointment != null) {
				for (Iterator<Map.Entry<Team, Integer>> itTeam = reviewAppointment.getTeams().entrySet().iterator(); 
						itTeam.hasNext();) {
					Team team = itTeam.next().getKey();
					if (team.getName().equals(teamName))
						itTeam.remove();
				}
				String reviewAppointmentExpr = reviewAppointment.toExpr();
				if (reviewAppointmentExpr != null)
					fileProtection.setReviewAppointmentExpr(reviewAppointmentExpr);
				else
					it.remove();
			} else {
				it.remove();
			}
		}
	}
	
	public void onAccountRename(Depot depot, String oldName, String newName) {
		ReviewAppointment reviewAppointment = getReviewAppointment(depot);
		if (reviewAppointment != null) {
			for (Account user: reviewAppointment.getUsers()) {
				if (user.getName().equals(oldName))
					user.setName(newName);
			}
			setReviewAppointmentExpr(reviewAppointment.toExpr());
		}
		
		for (Iterator<FileProtection> it = getFileProtections().iterator(); it.hasNext();) {
			FileProtection fileProtection = it.next();
			reviewAppointment = fileProtection.getReviewAppointment(depot);
			if (reviewAppointment != null) {
				for (Account user: reviewAppointment.getUsers()) {
					if (user.getName().equals(oldName))
						user.setName(newName);
				}
				fileProtection.setReviewAppointmentExpr(reviewAppointment.toExpr());
			} else {
				it.remove();
			}
		}		
	}
	
	public void onAccountDelete(Depot depot, String accountName) {
		ReviewAppointment reviewAppointment = getReviewAppointment(depot);
		if (reviewAppointment != null) {
			for (Iterator<Account> it = reviewAppointment.getUsers().iterator(); it.hasNext();) {
				Account user = it.next();
				if (user.getName().equals(accountName))
					it.remove();
			}
			setReviewAppointmentExpr(reviewAppointment.toExpr());
		}
		
		for (Iterator<FileProtection> it = getFileProtections().iterator(); it.hasNext();) {
			FileProtection fileProtection = it.next();
			reviewAppointment = fileProtection.getReviewAppointment(depot);
			if (reviewAppointment != null) {
				for (Iterator<Account> itUser = reviewAppointment.getUsers().iterator(); itUser.hasNext();) {
					Account user = itUser.next();
					if (user.getName().equals(accountName))
						itUser.remove();
				}
				String reviewAppointmentExpr = reviewAppointment.toExpr();
				if (reviewAppointmentExpr != null)
					fileProtection.setReviewAppointmentExpr(reviewAppointmentExpr);
				else
					it.remove();
			} else {
				it.remove();
			}
		}
	}
	
	public boolean onBranchDelete(String branchName) {
		return branchName.equals(getBranch());
	}

}
