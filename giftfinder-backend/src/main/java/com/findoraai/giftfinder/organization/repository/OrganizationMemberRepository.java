package com.findoraai.giftfinder.organization.repository;

import com.findoraai.giftfinder.organization.model.Organization;
import com.findoraai.giftfinder.organization.model.OrganizationMember;
import com.findoraai.giftfinder.organization.model.OrganizationRole;
import com.findoraai.giftfinder.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {
    List<OrganizationMember> findByOrganization(Organization organization);
    List<OrganizationMember> findByUser(User user);
    Optional<OrganizationMember> findByOrganizationAndUser(Organization organization, User user);
    boolean existsByOrganizationAndUserAndRole(Organization organization, User user, OrganizationRole role);
}
