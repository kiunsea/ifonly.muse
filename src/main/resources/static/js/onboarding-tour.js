(function () {
    const GUIDE_STORAGE_PREFIX = 'museAgentGuideSeen.v2';

    const i18n = Object.assign(
        {
            dashboardTitle: 'Muse Agent Dashboard',
            dashboardDesc: 'Manage local agent status, schedules, and devices in one place.',
            statusTitle: 'Current Status',
            statusDesc: 'Check Echo Server connection status quickly.',
            aliveTitle: 'Alive Status',
            aliveDesc: 'Review alive state and recent events.',
            taskTitle: 'Task Management',
            taskDesc: 'Manage cleanup paths and execution tasks.',
            echoConfigTitle: 'Echo Server Config',
            echoConfigDesc: 'Adjust connection info and Alive-Check settings.',
            deviceTitle: 'Device Registration',
            deviceDesc: 'Register this device to activate Echo integration.',
            guideTitle: 'Replay Guide',
            guideDesc: 'Use this button anytime to replay onboarding.',
            echoPageTitle: 'Echo Config Page',
            echoPageDesc: 'Configure credentials, tests, and Alive-Check intervals.',
            envTitle: 'Paste Config String',
            envDesc: 'Paste issued key-value settings and parse them automatically.',
            credentialTitle: 'Connection Fields',
            credentialDesc: 'Fill URL, Client ID, and Client Secret.',
            saveTestTitle: 'Save and Test',
            saveTestDesc: 'Save inputs and run a connection test immediately.',
            intervalTitle: 'Alive-Check Interval',
            intervalDesc: 'Tune polling interval and threshold days.',
            fullTestTitle: 'Full Connection Test',
            fullTestDesc: 'Validate communication with current settings.',
            devicePageTitle: 'Device Registration Page',
            devicePageDesc: 'Manage current registration state and device onboarding from here.',
            deviceCurrentTitle: 'Current Device Info',
            deviceCurrentDesc: 'Review the registered device ID, name, and OS details.',
            deviceRegisterTitle: 'Registration Form',
            deviceRegisterDesc: 'Enter a device name and register this Muse Agent with Echo Server.',
            deviceReregisterTitle: 'Re-registration',
            deviceReregisterDesc: 'Issue a fresh registration when the current device identity must change.',
            deviceResultTitle: 'Result Panel',
            deviceResultDesc: 'Check the registration or unregistration response and next steps.',
            cleanupPageTitle: 'Cleanup Path Management',
            cleanupPageDesc: 'Configure cleanup paths and trash retention policies.',
            cleanupSummaryTitle: 'Cleanup Summary',
            cleanupSummaryDesc: 'See how many cleanup paths exist and how many are enabled.',
            cleanupPathsTitle: 'Registered Paths',
            cleanupPathsDesc: 'Inspect paths that participate in scheduled cleanup.',
            cleanupAddTitle: 'Add New Path',
            cleanupAddDesc: 'Register an absolute file or directory path for cleanup.',
            cleanupVerifyTitle: 'Path Verification',
            cleanupVerifyDesc: 'Validate active paths before scheduled tasks use them.',
            cleanupTrashTitle: 'Trash Settings',
            cleanupTrashDesc: 'Tune retention days, trash root, and purge behavior.',
            taskHistoryPageTitle: 'Task History Page',
            taskHistoryPageDesc: 'Inspect scheduled and manual execution records with filters.',
            taskHistoryPresetsTitle: 'Quick Presets',
            taskHistoryPresetsDesc: 'Apply common filter combinations for routine investigations.',
            taskHistoryFiltersTitle: 'Detailed Filters',
            taskHistoryFiltersDesc: 'Refine records by group, task key, status, and date range.',
            taskHistoryLoadTitle: 'Load History',
            taskHistoryLoadDesc: 'Fetch task history using the active filter settings.',
            taskStatusTitle: 'Task Status',
            taskStatusDesc: 'Quick summary of recent task runs with a link to the full execution history page.',
            collapseToggleTitle: 'Collapse / Expand All',
            collapseToggleDesc: 'Collapse or expand every card inside the system panel at once.',
            cleanupNavTitle: 'Cleanup Paths',
            cleanupNavDesc: 'Open the dedicated page to register and edit cleanup paths.',
            taskHistoryNavTitle: 'Task Execution History',
            taskHistoryNavDesc: 'Open the detailed execution log page for every automatic and manual task.'
        },
        window.MUSE_TOUR_I18N || {}
    );

    function getDriverFactory() {
        return window.driver && window.driver.js && window.driver.js.driver;
    }

    function getPageKey() {
        const path = window.location.pathname || '/';
        if (path === '/echo-config') {
            return 'echo-config.v1';
        }
        if (path === '/device/register') {
            return 'device-register.v1';
        }
        if (path === '/cleanup') {
            return 'cleanup.v1';
        }
        if (path === '/task-history') {
            return 'task-history.v1';
        }
        return 'dashboard.v1';
    }

    function buildDashboardSteps() {
        const candidates = [
            {
                element: 'h1 a[href="/"]',
                popover: {
                    title: i18n.dashboardTitle,
                    description: i18n.dashboardDesc
                }
            },
            {
                element: '#btnToggleAll',
                popover: {
                    title: i18n.collapseToggleTitle,
                    description: i18n.collapseToggleDesc
                }
            },
            {
                element: '#statusSection',
                popover: {
                    title: i18n.statusTitle,
                    description: i18n.statusDesc
                }
            },
            {
                element: '#aliveStatusSection',
                popover: {
                    title: i18n.aliveTitle,
                    description: i18n.aliveDesc
                }
            },
            {
                element: '#btnRefreshAlive',
                popover: {
                    title: i18n.aliveTitle,
                    description: i18n.aliveDesc
                }
            },
            {
                element: 'section[data-card-id="task-mgmt"]',
                popover: {
                    title: i18n.taskStatusTitle,
                    description: i18n.taskStatusDesc
                }
            },
            {
                element: '#btnManageCleanup',
                popover: {
                    title: i18n.cleanupNavTitle,
                    description: i18n.cleanupNavDesc
                }
            },
            {
                element: '#btnOpenTaskHistoryPage',
                popover: {
                    title: i18n.taskHistoryNavTitle,
                    description: i18n.taskHistoryNavDesc
                }
            },
            {
                element: '#btnManageEchoConfig',
                popover: {
                    title: i18n.echoConfigTitle,
                    description: i18n.echoConfigDesc
                }
            },
            {
                element: '#btnDeviceManage',
                popover: {
                    title: i18n.deviceTitle,
                    description: i18n.deviceDesc
                }
            },
            {
                element: '#btnStartGuide',
                popover: {
                    title: i18n.guideTitle,
                    description: i18n.guideDesc
                }
            }
        ];

        // 시스템 · 설정 패널 가이드는 패널 내부 카드만 안내한다 (헤더 / Echo Note hero 등 외부는 제외).
        // 패널이 페이지에 없으면 (방어적) 기존 동작 유지.
        const systemPanel = document.getElementById('museSystemPanel');
        return candidates.filter(function (step) {
            const el = document.querySelector(step.element);
            if (!el) return false;
            if (systemPanel && !systemPanel.contains(el)) return false;
            return true;
        });
    }

    function buildEchoConfigSteps() {
        const candidates = [
            {
                element: 'h1',
                popover: {
                    title: i18n.echoPageTitle,
                    description: i18n.echoPageDesc
                }
            },
            {
                element: '#echoConnectionInfoSection',
                popover: {
                    title: i18n.echoPageTitle,
                    description: i18n.echoPageDesc
                }
            },
            {
                element: '#echoCredentialSection',
                popover: {
                    title: i18n.credentialTitle,
                    description: i18n.credentialDesc
                }
            },
            {
                element: '#envPasteArea',
                popover: {
                    title: i18n.envTitle,
                    description: i18n.envDesc
                }
            },
            {
                element: '#btnParseEnv',
                popover: {
                    title: i18n.envTitle,
                    description: i18n.envDesc
                }
            },
            {
                element: '#inputEchoUrl',
                popover: {
                    title: i18n.credentialTitle,
                    description: i18n.credentialDesc
                }
            },
            {
                element: '#btnSaveEchoCredentials',
                popover: {
                    title: i18n.saveTestTitle,
                    description: i18n.saveTestDesc
                }
            },
            {
                element: '#aliveCheckSettingsSection',
                popover: {
                    title: i18n.intervalTitle,
                    description: i18n.intervalDesc
                }
            },
            {
                element: '#inputIntervalMs',
                popover: {
                    title: i18n.intervalTitle,
                    description: i18n.intervalDesc
                }
            },
            {
                element: '#btnSaveSettings',
                popover: {
                    title: i18n.intervalTitle,
                    description: i18n.intervalDesc
                }
            },
            {
                element: '#echoCommTestSection',
                popover: {
                    title: i18n.fullTestTitle,
                    description: i18n.fullTestDesc
                }
            },
            {
                element: '#btnTestConnection',
                popover: {
                    title: i18n.fullTestTitle,
                    description: i18n.fullTestDesc
                }
            },
            {
                element: '#btnStartGuide',
                popover: {
                    title: i18n.guideTitle,
                    description: i18n.guideDesc
                }
            }
        ];

        return candidates.filter(function (step) {
            return !!document.querySelector(step.element);
        });
    }

    function buildDeviceRegisterSteps() {
        const candidates = [
            {
                element: 'h1',
                popover: {
                    title: i18n.devicePageTitle,
                    description: i18n.devicePageDesc
                }
            },
            {
                element: '#registrationSection',
                popover: {
                    title: i18n.deviceRegisterTitle,
                    description: i18n.deviceRegisterDesc
                }
            },
            {
                element: '#deviceName',
                popover: {
                    title: i18n.deviceRegisterTitle,
                    description: i18n.deviceRegisterDesc
                }
            },
            {
                element: '#btnRegister',
                popover: {
                    title: i18n.deviceRegisterTitle,
                    description: i18n.deviceRegisterDesc
                }
            },
            {
                element: '#currentRegistrationSection',
                popover: {
                    title: i18n.deviceCurrentTitle,
                    description: i18n.deviceCurrentDesc
                }
            },
            {
                element: '#btnShowReregister',
                popover: {
                    title: i18n.deviceReregisterTitle,
                    description: i18n.deviceReregisterDesc
                }
            },
            {
                element: '#reregisterSection',
                popover: {
                    title: i18n.deviceReregisterTitle,
                    description: i18n.deviceReregisterDesc
                }
            },
            {
                element: '#btnReregister',
                popover: {
                    title: i18n.deviceReregisterTitle,
                    description: i18n.deviceReregisterDesc
                }
            },
            {
                element: '#btnUnregister',
                popover: {
                    title: i18n.deviceCurrentTitle,
                    description: i18n.deviceCurrentDesc
                }
            },
            {
                element: '#resultContent',
                popover: {
                    title: i18n.deviceResultTitle,
                    description: i18n.deviceResultDesc
                }
            },
            {
                element: '#btnStartGuide',
                popover: {
                    title: i18n.guideTitle,
                    description: i18n.guideDesc
                }
            }
        ];

        return candidates.filter(function (step) {
            return !!document.querySelector(step.element);
        });
    }

    function buildCleanupSteps() {
        const candidates = [
            {
                element: 'h1',
                popover: {
                    title: i18n.cleanupPageTitle,
                    description: i18n.cleanupPageDesc
                }
            },
            {
                element: '#cleanupSummarySection',
                popover: {
                    title: i18n.cleanupSummaryTitle,
                    description: i18n.cleanupSummaryDesc
                }
            },
            {
                element: '#cleanupPathsSection',
                popover: {
                    title: i18n.cleanupPathsTitle,
                    description: i18n.cleanupPathsDesc
                }
            },
            {
                element: '#addPathSection',
                popover: {
                    title: i18n.cleanupAddTitle,
                    description: i18n.cleanupAddDesc
                }
            },
            {
                element: '#newPath',
                popover: {
                    title: i18n.cleanupAddTitle,
                    description: i18n.cleanupAddDesc
                }
            },
            {
                element: '#btnAdd',
                popover: {
                    title: i18n.cleanupAddTitle,
                    description: i18n.cleanupAddDesc
                }
            },
            {
                element: '#verifyPathsSection',
                popover: {
                    title: i18n.cleanupVerifyTitle,
                    description: i18n.cleanupVerifyDesc
                }
            },
            {
                element: '#btnVerify',
                popover: {
                    title: i18n.cleanupVerifyTitle,
                    description: i18n.cleanupVerifyDesc
                }
            },
            {
                element: '#trashSettingsSection',
                popover: {
                    title: i18n.cleanupTrashTitle,
                    description: i18n.cleanupTrashDesc
                }
            },
            {
                element: '#trashRetentionDays',
                popover: {
                    title: i18n.cleanupTrashTitle,
                    description: i18n.cleanupTrashDesc
                }
            },
            {
                element: '#btnSaveTrashSettings',
                popover: {
                    title: i18n.cleanupTrashTitle,
                    description: i18n.cleanupTrashDesc
                }
            },
            {
                element: '#trashStatusFilter',
                popover: {
                    title: i18n.cleanupTrashTitle,
                    description: i18n.cleanupTrashDesc
                }
            },
            {
                element: '#btnStartGuide',
                popover: {
                    title: i18n.guideTitle,
                    description: i18n.guideDesc
                }
            }
        ];

        return candidates.filter(function (step) {
            return !!document.querySelector(step.element);
        });
    }

    function buildTaskHistorySteps() {
        const candidates = [
            {
                element: 'h1',
                popover: {
                    title: i18n.taskHistoryPageTitle,
                    description: i18n.taskHistoryPageDesc
                }
            },
            {
                element: '#taskHistoryPresets',
                popover: {
                    title: i18n.taskHistoryPresetsTitle,
                    description: i18n.taskHistoryPresetsDesc
                }
            },
            {
                element: '#taskHistoryFilters',
                popover: {
                    title: i18n.taskHistoryFiltersTitle,
                    description: i18n.taskHistoryFiltersDesc
                }
            },
            {
                element: '#taskHistoryTaskKey',
                popover: {
                    title: i18n.taskHistoryFiltersTitle,
                    description: i18n.taskHistoryFiltersDesc
                }
            },
            {
                element: '#taskHistoryStatus',
                popover: {
                    title: i18n.taskHistoryFiltersTitle,
                    description: i18n.taskHistoryFiltersDesc
                }
            },
            {
                element: '#btnTaskHistory',
                popover: {
                    title: i18n.taskHistoryLoadTitle,
                    description: i18n.taskHistoryLoadDesc
                }
            },
            {
                element: '#taskHistoryContent',
                popover: {
                    title: i18n.taskHistoryLoadTitle,
                    description: i18n.taskHistoryLoadDesc
                }
            },
            {
                element: '#btnStartGuide',
                popover: {
                    title: i18n.guideTitle,
                    description: i18n.guideDesc
                }
            }
        ];

        return candidates.filter(function (step) {
            return !!document.querySelector(step.element);
        });
    }

    function buildSteps(pageKey) {
        if (pageKey === 'echo-config.v1') {
            return buildEchoConfigSteps();
        }
        if (pageKey === 'device-register.v1') {
            return buildDeviceRegisterSteps();
        }
        if (pageKey === 'cleanup.v1') {
            return buildCleanupSteps();
        }
        if (pageKey === 'task-history.v1') {
            return buildTaskHistorySteps();
        }
        return buildDashboardSteps();
    }

    function getStorageKey(pageKey) {
        return GUIDE_STORAGE_PREFIX + '.' + pageKey;
    }

    function startGuide(forceStart) {
        const pageKey = getPageKey();
        const storageKey = getStorageKey(pageKey);

        if (!forceStart && localStorage.getItem(storageKey) === 'true') {
            return;
        }

        const driverFactory = getDriverFactory();
        if (!driverFactory) {
            return;
        }

        const steps = buildSteps(pageKey);
        if (steps.length === 0) {
            return;
        }

        // Phase 5b: 투어 진행 시 시스템 패널 자동 열기·닫기.
        // 대부분의 투어 highlight 가 패널 내부 카드를 가리키므로, 시작 시 패널을 열어
        // 모든 step 이 보이도록 하고, 종료 시 원래 상태(닫힘)로 복귀.
        const panelWasOpenAtStart =
            document.getElementById('museSystemPanel')
            && document.getElementById('museSystemPanel').classList.contains('open');

        if (typeof openSystemPanel === 'function' && !panelWasOpenAtStart) {
            openSystemPanel();
        }

        const guide = driverFactory({
            showProgress: true,
            animate: true,
            allowClose: true,
            steps: steps,
            onDestroyed: function () {
                localStorage.setItem(storageKey, 'true');
                // 투어 시작 시 패널이 닫혀 있었다면 다시 닫기 (사용자 환경 보존)
                if (typeof closeSystemPanel === 'function' && !panelWasOpenAtStart) {
                    closeSystemPanel();
                }
            }
        });

        guide.drive();
    }

    document.addEventListener('DOMContentLoaded', function () {
        const btn = document.getElementById('btnStartGuide');
        if (btn) {
            btn.addEventListener('click', function () {
                startGuide(true);
            });
        }
    });
})();
