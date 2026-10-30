package com.upc.pre.peaceapp.security.iam.application.internal.queryservices;

import com.upc.pre.peaceapp.security.iam.domain.model.aggregates.User;
import com.upc.pre.peaceapp.security.iam.domain.model.queries.GetAllUsersQuery;
import com.upc.pre.peaceapp.security.iam.domain.services.UserIAMQueryService;
import com.upc.pre.peaceapp.security.iam.infrastructure.persistence.jpa.repositories.UserIAMRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserIAMQueryServiceImpl implements UserIAMQueryService {
    private final UserIAMRepository userRepository;

    public UserIAMQueryServiceImpl(UserIAMRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<User> handle(GetAllUsersQuery query) {
        return userRepository.findAll();
    }
}
