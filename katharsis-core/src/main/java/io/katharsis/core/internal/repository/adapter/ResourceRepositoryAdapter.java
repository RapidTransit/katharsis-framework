package io.katharsis.core.internal.repository.adapter;

import java.io.Serializable;

import io.katharsis.legacy.internal.AnnotatedResourceRepositoryAdapter;
import io.katharsis.legacy.repository.ResourceRepository;
import io.katharsis.module.ModuleRegistry;
import io.katharsis.queryspec.QuerySpec;
import io.katharsis.repository.ResourceRepositoryV2;
import io.katharsis.repository.filter.RepositoryFilterContext;
import io.katharsis.repository.ResourceRepositoryV2;
import io.katharsis.repository.request.HttpMethod;
import io.katharsis.repository.request.QueryAdapter;
import io.katharsis.repository.request.RepositoryRequestSpec;
import io.katharsis.repository.response.JsonApiResponse;
import io.katharsis.resource.information.ResourceInformation;

/**
 * A repository adapter for resource repository.
 */
@SuppressWarnings("unchecked")
public class ResourceRepositoryAdapter<T, I extends Serializable> extends ResponseRepositoryAdapter {

	private final Object resourceRepository;

	private final boolean isAnnotated;

	public ResourceRepositoryAdapter(ResourceInformation resourceInformation, ModuleRegistry moduleRegistry, Object resourceRepository) {
		super(resourceInformation, moduleRegistry);
		this.resourceRepository = resourceRepository;
		this.isAnnotated = resourceRepository instanceof AnnotatedResourceRepositoryAdapter;
	}

	public JsonApiResponse findOne(I id, QueryAdapter queryAdapter) {
		RepositoryRequestFilterChainImpl chain = new RepositoryRequestFilterChainImpl() {

			@SuppressWarnings("rawtypes")
			@Override
			protected JsonApiResponse invoke(RepositoryFilterContext context) {
				RepositoryRequestSpec request = context.getRequest();
				QueryAdapter queryAdapter = request.getQueryAdapter();
				Serializable id = request.getId();
				Object resource;
				if (isAnnotated) {
					resource = ((AnnotatedResourceRepositoryAdapter) resourceRepository).findOne(id, queryAdapter);
				} else if (resourceRepository instanceof ResourceRepositoryV2) {
					resource = ((ResourceRepositoryV2) resourceRepository).findOne(id, request.getQuerySpec(resourceInformation));
				} else {
					resource = ((ResourceRepository) resourceRepository).findOne(id, request.getQueryParams());
				}
				return getResponse(resourceRepository, resource, request);
			}

		};
		RepositoryRequestSpec requestSpec = RepositoryRequestSpecImpl.forFindId(moduleRegistry, queryAdapter, id);
		return chain.doFilter(newRepositoryFilterContext(requestSpec));
	}

	public JsonApiResponse findAll(QueryAdapter queryAdapter) {
		RepositoryRequestFilterChainImpl chain = new RepositoryRequestFilterChainImpl() {

			@SuppressWarnings("rawtypes")
			@Override
			protected JsonApiResponse invoke(RepositoryFilterContext context) {
				RepositoryRequestSpec request = context.getRequest();
				QueryAdapter queryAdapter = request.getQueryAdapter();
				Object resources;
				if (isAnnotated) {
					resources = ((AnnotatedResourceRepositoryAdapter) resourceRepository).findAll(queryAdapter);
				} else if (resourceRepository instanceof ResourceRepositoryV2) {
					QuerySpec querySpec = request.getQuerySpec(resourceInformation);
					resources = ((ResourceRepositoryV2) resourceRepository).findAll(querySpec);
				} else {
					resources = ((ResourceRepository) resourceRepository).findAll(request.getQueryParams());
				}
				return getResponse(resourceRepository, resources, request);
			}

		};
		RepositoryRequestSpec requestSpec = RepositoryRequestSpecImpl.forFindAll(moduleRegistry, queryAdapter);
		return chain.doFilter(newRepositoryFilterContext(requestSpec));
	}

	public JsonApiResponse findAll(Iterable ids, QueryAdapter queryAdapter) {
		RepositoryRequestFilterChainImpl chain = new RepositoryRequestFilterChainImpl() {

			@SuppressWarnings("rawtypes")
			@Override
			protected JsonApiResponse invoke(RepositoryFilterContext context) {
				RepositoryRequestSpec request = context.getRequest();
				QueryAdapter queryAdapter = request.getQueryAdapter();
				Iterable<?> ids = request.getIds();
				Object resources;
				if (isAnnotated) {
					resources = ((AnnotatedResourceRepositoryAdapter) resourceRepository).findAll(ids, queryAdapter);
				} else if (resourceRepository instanceof ResourceRepositoryV2) {
					resources = ((ResourceRepositoryV2) resourceRepository).findAll(ids, request.getQuerySpec(resourceInformation));
				} else {
					resources = ((ResourceRepository) resourceRepository).findAll(ids, request.getQueryParams());
				}
				return getResponse(resourceRepository, resources, request);
			}

		};
		RepositoryRequestSpec requestSpec = RepositoryRequestSpecImpl.forFindIds(moduleRegistry, queryAdapter, ids);
		return chain.doFilter(newRepositoryFilterContext(requestSpec));
	}

	public <S extends T> JsonApiResponse update(S entity, QueryAdapter queryAdapter) {
		return save(entity, queryAdapter, HttpMethod.PATCH);
	}

	public <S extends T> JsonApiResponse create(S entity, QueryAdapter queryAdapter) {
		return save(entity, queryAdapter, HttpMethod.POST);
	}

	private <S extends T> JsonApiResponse save(S entity, QueryAdapter queryAdapter, final HttpMethod method) {
		RepositoryRequestFilterChainImpl chain = new RepositoryRequestFilterChainImpl() {

			@SuppressWarnings("rawtypes")
			@Override
			protected JsonApiResponse invoke(RepositoryFilterContext context) {
				RepositoryRequestSpec request = context.getRequest();
				Object entity = request.getEntity();

				Object resource;
				if (isAnnotated) {
					resource = ((AnnotatedResourceRepositoryAdapter) resourceRepository).save(entity);
				} else if (resourceRepository instanceof ResourceRepositoryV2) {
					if (method == HttpMethod.POST) {
						resource = ((ResourceRepositoryV2) resourceRepository).create(entity);
					} else {
						resource = ((ResourceRepositoryV2) resourceRepository).save(entity);
					}
				} else if (resourceRepository instanceof ResourceRepositoryV2) {
					resource = ((ResourceRepositoryV2) resourceRepository).save(entity);
				} else {
					resource = ((ResourceRepository) resourceRepository).save(entity);
				}
				return getResponse(resourceRepository, resource, request);
			}

		};
		RepositoryRequestSpec requestSpec = RepositoryRequestSpecImpl.forSave(moduleRegistry, method, queryAdapter, entity);
		return chain.doFilter(newRepositoryFilterContext(requestSpec));
	}

	public JsonApiResponse delete(I id, QueryAdapter queryAdapter) {
		RepositoryRequestFilterChainImpl chain = new RepositoryRequestFilterChainImpl() {

			@SuppressWarnings("rawtypes")
			@Override
			protected JsonApiResponse invoke(RepositoryFilterContext context) {
				RepositoryRequestSpec request = context.getRequest();
				QueryAdapter queryAdapter = request.getQueryAdapter();
				Serializable id = request.getId();
				if (isAnnotated) {
					((AnnotatedResourceRepositoryAdapter) resourceRepository).delete(id, queryAdapter);
				} else if (resourceRepository instanceof ResourceRepositoryV2) {
					((ResourceRepositoryV2) resourceRepository).delete(id);
				} else {
					((ResourceRepository) resourceRepository).delete(id);
				}
				return new JsonApiResponse();
			}
		};
		RepositoryRequestSpec requestSpec = RepositoryRequestSpecImpl.forDelete(moduleRegistry, queryAdapter, id);
		return chain.doFilter(newRepositoryFilterContext(requestSpec));
	}

	public Object getResourceRepository() {
		return resourceRepository;
	}

	@Override
	protected ResourceInformation getResourceInformation(Object repository) {
		return resourceInformation;
	}
}
