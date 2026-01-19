package org.authzorium.client.service.impl;

import lombok.RequiredArgsConstructor;
import org.authzorium.client.dto.Pet;
import org.authzorium.client.repository.PetRepository;
import org.authzorium.client.view.PetView;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@ConditionalOnClass(name = "com.blazebit.persistence.view.EntityViewManager")
public class BlazePetService {
    private final ApplicationContext applicationContext;
    private final EntityManager em;
    private final PetRepository petRepository;

    public Page<Pet> findPetsByOwnerId(Long ownerId, Pageable pageable) {
        try {
            Class<?> cbfClass = Class.forName("com.blazebit.persistence.CriteriaBuilderFactory");
            Class<?> evmClass = Class.forName("com.blazebit.persistence.view.EntityViewManager");

            Object cbf = applicationContext.getBean(cbfClass);
            Object evm = applicationContext.getBean(evmClass);

            // cbf.create(em, PetView.class) -> returns com.blazebit.persistence.CriteriaBuilder
            Method createMethod = cbfClass.getMethod("create", java.lang.Object.class, java.lang.Class.class);
            Object cb = createMethod.invoke(cbf, em, PetView.class);

            // where("owner.id").eq(ownerId)
            Method whereMethod = cb.getClass().getMethod("where", String.class);
            Object whereCb = whereMethod.invoke(cb, "owner.id");
            Method eqMethod = whereCb.getClass().getMethod("eq", Object.class);
            eqMethod.invoke(whereCb, ownerId);

            // setFirstResult, setMaxResults
            Method setFirstResult = cb.getClass().getMethod("setFirstResult", int.class);
            Method setMaxResults = cb.getClass().getMethod("setMaxResults", int.class);
            setFirstResult.invoke(cb, (int) pageable.getOffset());
            setMaxResults.invoke(cb, pageable.getPageSize());

            // getResultList
            Method getResultList = cb.getClass().getMethod("getResultList");
            List<?> views = (List<?>) getResultList.invoke(cb);

            List<Pet> dtos = views.stream().map(v -> {
                try {
                    Method getId = v.getClass().getMethod("getId");
                    Method getPetName = v.getClass().getMethod("getPetName");
                    Method getOwner = v.getClass().getMethod("getOwner");
                    Object ownerView = getOwner.invoke(v);

                    Pet p = new Pet();
                    p.setId((Long) getId.invoke(v));
                    p.setPetName((String) getPetName.invoke(v));

                    if (ownerView != null) {
                        var user = new org.authzorium.client.dto.User();
                        Method getOwnerId = ownerView.getClass().getMethod("getId");
                        Method getUsername = ownerView.getClass().getMethod("getUsername");
                        Method getDisplayName = ownerView.getClass().getMethod("getDisplayName");
                        user.setId((Long) getOwnerId.invoke(ownerView));
                        user.setUsername((String) getUsername.invoke(ownerView));
                        user.setDisplayName((String) getDisplayName.invoke(ownerView));
                        p.setOwner(user);
                    }
                    return p;
                } catch (ReflectiveOperationException ex) {
                    throw new RuntimeException(ex);
                }
            }).toList();

            long total = petRepository.countByOwnerId(ownerId);
            return new PageImpl<>(dtos, pageable, total);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Blaze-Persistence is not available on the classpath", e);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Reflection failure while invoking Blaze APIs", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
