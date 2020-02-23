/* eslint-disable react/jsx-wrap-multilines */
import React, { useState } from 'react';
import authenticated from 'modules/auth/hoc/authenticated';
import AppLayout from 'modules/app/components/Layout';
import { Tabs, Tab, Icon } from '@blueprintjs/core';
import dynamic from 'next/dynamic';
import styled from 'styled-components';
import { BaseRouter } from 'next/dist/next-server/lib/router/router';

type Props = {
  url: BaseRouter;
};

const StyledGeneralSettingsSection = styled.div`
  .bp3-tab-panel {
    width: 100%;
    padding: 16px;
  }
`;

const LazySetupPanel = dynamic(() => import('components/settings/SetupPanel'));
const LazyTeamPanel = dynamic(() => import('components/settings/TeamPanel'));

const GeneralSettings = ({ url }: Props) => {
  const [selectedTabId, setSelectedTabId] = useState<React.ReactText>(
    '/settings/general'
  );

  const handleTabChange = (newTabId: React.ReactText) => {
    setSelectedTabId(newTabId);
  };

  return (
    <AppLayout pathname={url.pathname}>
      <StyledGeneralSettingsSection>
        <Tabs
          vertical
          onChange={handleTabChange}
          selectedTabId={selectedTabId}
          renderActiveTabPanelOnly
        >
          <Tab
            id="/settings/general"
            title={
              <>
                <Icon icon="build" />
                Setup Insight
              </>
            }
            panel={<LazySetupPanel />}
          />
          <Tab
            id="/settings/tracking"
            title={
              <>
                <Icon icon="record" />
                Tracking
              </>
            }
            panel={<div>Tracking</div>}
          />
          <Tab
            id="/settings/team"
            title={
              <>
                <Icon icon="people" />
                Team
              </>
            }
            panel={<LazyTeamPanel />}
          />
        </Tabs>
      </StyledGeneralSettingsSection>
    </AppLayout>
  );
};

export default authenticated(GeneralSettings);
