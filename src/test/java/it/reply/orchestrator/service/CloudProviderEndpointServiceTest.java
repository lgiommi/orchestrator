package it.reply.orchestrator.service;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import it.reply.orchestrator.controller.ControllerTestUtils;
import it.reply.orchestrator.dal.entity.Deployment;
import it.reply.orchestrator.dto.CloudProvider;
import it.reply.orchestrator.dto.CloudProviderEndpoint;
import it.reply.orchestrator.dto.RankCloudProvidersMessage;
import it.reply.orchestrator.dto.CloudProviderEndpoint.IaaSType;
import it.reply.orchestrator.dto.cmdb.CloudService;
import it.reply.orchestrator.dto.cmdb.CloudServiceData;
import it.reply.orchestrator.dto.cmdb.CmdbDataWrapper;
import it.reply.orchestrator.dto.cmdb.Type;
import it.reply.orchestrator.dto.deployment.AwsSlaPlacementPolicy;
import it.reply.orchestrator.dto.deployment.CredentialsAwareSlaPlacementPolicy;
import it.reply.orchestrator.dto.deployment.PlacementPolicy;
import it.reply.orchestrator.dto.ranker.RankedCloudProvider;
import it.reply.orchestrator.exception.OrchestratorException;
import it.reply.orchestrator.exception.service.DeploymentException;
import it.reply.utils.json.JsonUtility;

public class CloudProviderEndpointServiceTest {

  @InjectMocks
  CloudProviderEndpointServiceImpl cloudProviderEndpointServiceImpl =
      new CloudProviderEndpointServiceImpl();

  @Test
  public void chooseCloudProvider() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();
    RankCloudProvidersMessage rcpm = new RankCloudProvidersMessage();

    List<RankedCloudProvider> listRankedCloudProvider = new ArrayList<>();

    RankedCloudProvider rankedCloudProvider1 = new RankedCloudProvider();
    rankedCloudProvider1.setRank(100);
    RankedCloudProvider rankedCloudProvider2 = new RankedCloudProvider();
    rankedCloudProvider2.setRank(400);
    RankedCloudProvider rankedCloudProvider3 = new RankedCloudProvider();
    rankedCloudProvider3.setRank(200);

    listRankedCloudProvider.add(rankedCloudProvider1);
    listRankedCloudProvider.add(rankedCloudProvider2);
    listRankedCloudProvider.add(rankedCloudProvider3);

    rcpm.setRankedCloudProviders(listRankedCloudProvider);

    assertEquals(cloudProviderEndpointServiceImpl.chooseCloudProvider(deployment, rcpm),
        rankedCloudProvider2);
  }

  @Test(expected = DeploymentException.class)
  public void failChooseCloudProvider() throws Exception {
    Deployment deployment = ControllerTestUtils.createDeployment();
    RankCloudProvidersMessage rcpm = new RankCloudProvidersMessage();
    cloudProviderEndpointServiceImpl.chooseCloudProvider(deployment, rcpm);
  }

  @Test(expected = IllegalArgumentException.class)
  public void failGetCloudProviderEndpoint() {
    CloudProvider chosenCloudProvider = new CloudProvider();
    List<PlacementPolicy> placementPolicies = new ArrayList<>();

    cloudProviderEndpointServiceImpl.getCloudProviderEndpoint(chosenCloudProvider,
        placementPolicies);
  }

  @Test
  public void getCloudProviderEndpoint() {

    // OCCI
    List<PlacementPolicy> placementPolicies = new ArrayList<>();
    List<CloudService> cmbdProviderServicesByType = new ArrayList<>();

    CloudProvider chosenCloudProvider = Mockito.mock(CloudProvider.class);

    CloudServiceData cloudServiceDataOCC = new CloudServiceData();
    cloudServiceDataOCC.setServiceType("eu.egi.cloud.vm-management.occi");
    cloudServiceDataOCC.setEndpoint("www.endpoint.com");

    CloudService cloudService = new CloudService();
    cloudService.setId(UUID.randomUUID().toString());
    cloudService.setData(cloudServiceDataOCC);
    cmbdProviderServicesByType.add(cloudService);

    Mockito.when(chosenCloudProvider.getCmbdProviderServicesByType(Type.COMPUTE))
        .thenReturn(cmbdProviderServicesByType);

    // Result OCCI
    CloudProviderEndpoint result = new CloudProviderEndpoint();
    result.setCpEndpoint(cloudService.getData().getEndpoint());
    result.setCpComputeServiceId(cloudService.getId());
    result.setIaasType(IaaSType.OCCI);

    Assert.assertEquals(cloudProviderEndpointServiceImpl
        .getCloudProviderEndpoint(chosenCloudProvider, placementPolicies), result);

    // OPENSTACK

    cloudServiceDataOCC.setServiceType("eu.egi.cloud.vm-management.openstack");
    cloudService.setData(cloudServiceDataOCC);
    cmbdProviderServicesByType.clear();
    cmbdProviderServicesByType.add(cloudService);
    Mockito.when(chosenCloudProvider.getCmbdProviderServicesByType(Type.COMPUTE))
        .thenReturn(cmbdProviderServicesByType);

    // result OPENSTACK
    result.setIaasType(IaaSType.OPENSTACK);

    Assert.assertEquals(cloudProviderEndpointServiceImpl
        .getCloudProviderEndpoint(chosenCloudProvider, placementPolicies), result);

    // OPENNEBULA

    cloudServiceDataOCC.setServiceType("eu.egi.cloud.vm-management.opennebula");
    cloudService.setData(cloudServiceDataOCC);
    cmbdProviderServicesByType.clear();
    cmbdProviderServicesByType.add(cloudService);
    Mockito.when(chosenCloudProvider.getCmbdProviderServicesByType(Type.COMPUTE))
        .thenReturn(cmbdProviderServicesByType);

    // result OPENEBULA
    result.setIaasType(IaaSType.OPENNEBULA);

    Assert.assertEquals(cloudProviderEndpointServiceImpl
        .getCloudProviderEndpoint(chosenCloudProvider, placementPolicies), result);

    // AWS Not covered because contains a lambda expression

    cloudServiceDataOCC.setServiceType("com.amazonaws.ec2");
    cloudService.setData(cloudServiceDataOCC);
    cmbdProviderServicesByType.clear();
    cmbdProviderServicesByType.add(cloudService);
    Mockito.when(chosenCloudProvider.getCmbdProviderServicesByType(Type.COMPUTE))
        .thenReturn(cmbdProviderServicesByType);
    AwsSlaPlacementPolicy awsSlaPlacementPolicy =
        new AwsSlaPlacementPolicy(new CredentialsAwareSlaPlacementPolicy(new ArrayList<>(),
            UUID.randomUUID().toString(), "username", "password"));
    placementPolicies.add(awsSlaPlacementPolicy);

    result.setIaasType(IaaSType.AWS);
    result.setUsername(awsSlaPlacementPolicy.getAccessKey());
    result.setPassword(awsSlaPlacementPolicy.getSecretKey());
    Assert.assertEquals(cloudProviderEndpointServiceImpl
        .getCloudProviderEndpoint(chosenCloudProvider, placementPolicies), result);

  }

  @Test(expected = OrchestratorException.class)
  public void failGetCloudProviderEndpointFailNoAWSCredentialProvider() {
    List<PlacementPolicy> placementPolicies = new ArrayList<>();

    List<CloudService> cmbdProviderServicesByType = new ArrayList<>();

    CloudProvider chosenCloudProvider = Mockito.mock(CloudProvider.class);

    CloudServiceData cloudServiceDataOCC = new CloudServiceData();
    cloudServiceDataOCC.setServiceType("com.amazonaws.ec2");
    cloudServiceDataOCC.setEndpoint("www.endpoint.com");

    CloudService cloudService = new CloudService();
    cloudService.setId(UUID.randomUUID().toString());
    cloudService.setData(cloudServiceDataOCC);
    cmbdProviderServicesByType.add(cloudService);

    Mockito.when(chosenCloudProvider.getCmbdProviderServicesByType(Type.COMPUTE))
        .thenReturn(cmbdProviderServicesByType);


    cloudProviderEndpointServiceImpl.getCloudProviderEndpoint(chosenCloudProvider,
        placementPolicies);
  }

  @Test(expected = IllegalArgumentException.class)
  public void failGetCloudProviderEndpointIllegalArgument() {
    List<PlacementPolicy> placementPolicies = new ArrayList<>();

    List<CloudService> cmbdProviderServicesByType = new ArrayList<>();

    CloudProvider chosenCloudProvider = Mockito.mock(CloudProvider.class);

    CloudServiceData cloudServiceDataOCC = new CloudServiceData();
    cloudServiceDataOCC.setServiceType("com.amazonaws.lorem.ipsum");
    cloudServiceDataOCC.setEndpoint("www.endpoint.com");

    CloudService cloudService = new CloudService();
    cloudService.setId(UUID.randomUUID().toString());
    cloudService.setData(cloudServiceDataOCC);
    cmbdProviderServicesByType.add(cloudService);

    Mockito.when(chosenCloudProvider.getCmbdProviderServicesByType(Type.COMPUTE))
        .thenReturn(cmbdProviderServicesByType);

    cloudProviderEndpointServiceImpl.getCloudProviderEndpoint(chosenCloudProvider,
        placementPolicies);
  }

}
