package com.upc.pre.peaceapp.security.iam.domain.services;

import com.upc.pre.peaceapp.security.iam.domain.model.aggregates.User;
import com.upc.pre.peaceapp.security.iam.domain.model.queries.GetAllUsersQuery;

import java.util.List;

public interface UserIAMQueryService {
    List<User> handle(GetAllUsersQuery query);
}
