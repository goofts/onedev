package com.pmease.gitplex.core.event.pullrequest;

import java.util.Date;

import com.pmease.gitplex.core.entity.Account;
import com.pmease.gitplex.core.entity.PullRequestStatusChange;
import com.pmease.gitplex.core.event.MarkdownAware;

public class PullRequestStatusChangeEvent extends PullRequestChangeEvent implements MarkdownAware {

	private final PullRequestStatusChange statusChange;
	
	public PullRequestStatusChangeEvent(PullRequestStatusChange statusChange) {
		super(statusChange.getRequest());
		this.statusChange = statusChange;
	}

	public PullRequestStatusChange getStatusChange() {
		return statusChange;
	}

	@Override
	public String getMarkdown() {
		return statusChange.getNote();
	}

	@Override
	public Account getUser() {
		return statusChange.getUser();
	}

	@Override
	public Date getDate() {
		return statusChange.getDate();
	}

}
