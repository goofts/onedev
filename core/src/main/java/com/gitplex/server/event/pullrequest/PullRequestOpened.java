package com.gitplex.server.event.pullrequest;

import java.util.Date;

import com.gitplex.server.event.MarkdownAware;
import com.gitplex.server.model.Account;
import com.gitplex.server.model.PullRequest;
import com.gitplex.server.util.editable.annotation.Editable;

@Editable(name="opened")
public class PullRequestOpened extends PullRequestEvent implements MarkdownAware {

	public PullRequestOpened(PullRequest request) {
		super(request);
	}

	@Override
	public String getMarkdown() {
		return getRequest().getDescription();
	}

	@Override
	public Account getUser() {
		return Account.getForDisplay(getRequest().getSubmitter(), getRequest().getSubmitterName());
	}

	@Override
	public Date getDate() {
		return getRequest().getSubmitDate();
	}

}
