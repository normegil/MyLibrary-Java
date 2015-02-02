package be.normegil.mylibrary.tools.dao;

import be.normegil.mylibrary.tools.CustomCollectors;
import be.normegil.mylibrary.framework.rest.RESTMethod;
import be.normegil.mylibrary.framework.security.rightsmanagement.Right;
import be.normegil.mylibrary.framework.security.rightsmanagement.RightDAO;
import be.normegil.mylibrary.framework.security.rightsmanagement.group.Group;
import be.normegil.mylibrary.framework.security.rightsmanagement.resource.Resource;
import be.normegil.mylibrary.framework.security.rightsmanagement.resource.SpecificResource;
import be.normegil.mylibrary.user.User;

import javax.validation.constraints.NotNull;
import java.util.Optional;
import java.util.stream.Stream;

public class RightMemoryDAO extends MemoryDAO<Right> implements RightDAO {
	@Override
	protected boolean correspondingID(final Right right, final Object id) {
		return id.equals(right.getId());
	}

	@Override
	public Optional<Right> get(@NotNull final Group group, @NotNull final Resource resource, @NotNull final RESTMethod method) {
		Stream<Right> rightStream = getAll()
				.filter((r) -> group.equals(r.getGroup().get()));
		return getRight(rightStream, resource, method);
	}

	@Override
	public Optional<Right> get(@NotNull final User user, @NotNull final SpecificResource resource, @NotNull final RESTMethod method) {
		Stream<Right> rightStream = getAll()
				.filter((r) -> user.equals(r.getUser().get()));
		return getRight(rightStream, resource, method);
	}

	private Optional<Right> getRight(final Stream<Right> rightStream, final Resource resource, final RESTMethod method) {
		return rightStream
				.filter((r) -> resource.equals(r.getResource()) && method.equals(r.getMethod()))
				.collect(new CustomCollectors().singletonCollector());
	}

}