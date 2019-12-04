/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include "security/facade.h"
#include "hci/hci_layer.h"
#include "l2cap/classic/l2cap_classic_module.h"
#include "l2cap/le/l2cap_le_module.h"
#include "os/handler.h"
#include "security/facade.grpc.pb.h"
#include "security/security_module.h"

namespace bluetooth {
namespace security {

class SecurityModuleFacadeService : public SecurityModuleFacade::Service {
 public:
  SecurityModuleFacadeService(SecurityModule* security_module, l2cap::le::L2capLeModule* l2cap_le_module,
                              l2cap::classic::L2capClassicModule* l2cap_classic_module, hci::HciLayer* hci_layer,
                              ::bluetooth::os::Handler* security_handler)
      : security_module_(security_module), l2cap_le_module_(l2cap_le_module),
        l2cap_classic_module_(l2cap_classic_module), security_handler_(security_handler) {
    // TODO(optedoblivion): Register callback listener
  }

 private:
  SecurityModule* security_module_ __attribute__((unused));
  l2cap::le::L2capLeModule* l2cap_le_module_ __attribute__((unused));
  l2cap::classic::L2capClassicModule* l2cap_classic_module_ __attribute__((unused));
  ::bluetooth::os::Handler* security_handler_ __attribute__((unused));
};

void SecurityModuleFacadeModule::ListDependencies(ModuleList* list) {
  ::bluetooth::grpc::GrpcFacadeModule::ListDependencies(list);
  list->add<SecurityModule>();
  list->add<l2cap::le::L2capLeModule>();
  list->add<l2cap::classic::L2capClassicModule>();
  list->add<hci::HciLayer>();
}

void SecurityModuleFacadeModule::Start() {
  ::bluetooth::grpc::GrpcFacadeModule::Start();
  service_ = new SecurityModuleFacadeService(GetDependency<SecurityModule>(), GetDependency<l2cap::le::L2capLeModule>(),
                                             GetDependency<l2cap::classic::L2capClassicModule>(),
                                             GetDependency<hci::HciLayer>(), GetHandler());
}

void SecurityModuleFacadeModule::Stop() {
  delete service_;
  ::bluetooth::grpc::GrpcFacadeModule::Stop();
}

::grpc::Service* SecurityModuleFacadeModule::GetService() const {
  return service_;
}

const ModuleFactory SecurityModuleFacadeModule::Factory =
    ::bluetooth::ModuleFactory([]() { return new SecurityModuleFacadeModule(); });

}  // namespace security
}  // namespace bluetooth
