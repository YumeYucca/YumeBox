import {
  Card,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  Icon,
} from "@mintlify/components";
import { useEffect, useMemo, useRef, useState } from "react";
import backgroundImage from "./assets/background.png";
import androidLogo from "./assets/brands/android.svg";
import asusLogo from "./assets/brands/asus.svg";
import fairphoneLogo from "./assets/brands/fairphone.svg";
import googleLogo from "./assets/brands/google.svg";
import honorLogo from "./assets/brands/honor.svg";
import htcLogo from "./assets/brands/htc.svg";
import huaweiLogo from "./assets/brands/huawei.svg";
import kotlinLogo from "./assets/brands/kotlin.svg";
import lenovoLogo from "./assets/brands/lenovo.svg";
import lgLogo from "./assets/brands/lg.svg";
import meizuLogo from "./assets/brands/meizu.svg";
import motorolaLogo from "./assets/brands/motorola.svg";
import nokiaLogo from "./assets/brands/nokia.svg";
import oneplusLogo from "./assets/brands/oneplus.svg";
import oppoLogo from "./assets/brands/oppo.svg";
import samsungLogo from "./assets/brands/samsung.svg";
import sharpLogo from "./assets/brands/sharp.svg";
import sonyLogo from "./assets/brands/sony.svg";
import tecnoLogo from "./assets/brands/tecno.svg";
import vivoLogo from "./assets/brands/vivo.svg";
import xiaomiLogo from "./assets/brands/xiaomi.svg";
import zteLogo from "./assets/brands/zte.svg";
import activityIcon from "./assets/icons/activity.svg";
import calendarIcon from "./assets/icons/calendar.svg";
import usersIcon from "./assets/icons/users.svg";
import zapIcon from "./assets/icons/zap.svg";

type Distribution = {
  value: string;
  devices: number;
  launches: number;
};

type VersionDistribution = {
  version: string;
  versionCode: string;
  devices: number;
  launches: number;
};

type TrendPoint = {
  date: string;
  launches: number;
  activeDevices: number;
  newDevices: number;
};

type PublicMetrics = {
  generatedAt: string;
  days: number;
  overview: {
    totalDevices: number;
    totalLaunches: number;
    activeToday: number;
    activeThisWeek: number;
    newToday: number;
  };
  trend: TrendPoint[];
  distributions: {
    versions: VersionDistribution[];
    androidVersion: Distribution[];
    manufacturer: Distribution[];
  };
};

const RANGE_OPTIONS = [
  { days: 7, label: "最近 7 天", description: "本周活跃趋势", icon: "calendar-days" },
  { days: 30, label: "最近 30 天", description: "近一个月走势", icon: "calendar-range" },
  { days: 90, label: "最近 90 天", description: "近三个月走势", icon: "calendar" },
] as const;

type RangeDays = (typeof RANGE_OPTIONS)[number]["days"];

const metricDefinitions = [
  { key: "totalDevices", label: "累计设备", icon: usersIcon },
  { key: "totalLaunches", label: "累计启动", icon: zapIcon },
  { key: "activeToday", label: "今日活跃", icon: activityIcon },
  { key: "activeThisWeek", label: "近 7 日活跃", icon: calendarIcon },
] as const;

const OEM_LOGOS: Record<string, string> = {
  asus: asusLogo,
  fairphone: fairphoneLogo,
  google: googleLogo,
  honor: honorLogo,
  htc: htcLogo,
  huawei: huaweiLogo,
  lenovo: lenovoLogo,
  lg: lgLogo,
  meizu: meizuLogo,
  motorola: motorolaLogo,
  nokia: nokiaLogo,
  oneplus: oneplusLogo,
  oppo: oppoLogo,
  samsung: samsungLogo,
  sharp: sharpLogo,
  sony: sonyLogo,
  tecno: tecnoLogo,
  vivo: vivoLogo,
  xiaomi: xiaomiLogo,
  zte: zteLogo,
};

function oemLogo(name: string): string | undefined {
  return OEM_LOGOS[name.toLowerCase().replace(/[^a-z]/g, "")];
}

type Change = { dir: "up" | "down" | "flat"; text: string };

const FLAT: Change = { dir: "flat", text: "0%" };

function percentChange(current: number, previous: number): Change {
  if (previous <= 0) return current > 0 ? { dir: "up", text: "新增" } : FLAT;
  const pct = ((current - previous) / previous) * 100;
  if (Math.abs(pct) < 0.5) return FLAT;
  return { dir: pct > 0 ? "up" : "down", text: `${Math.round(Math.abs(pct))}%` };
}

function average(values: number[]): number {
  if (values.length === 0) return 0;
  return values.reduce((sum, value) => sum + value, 0) / values.length;
}

function number(value: number): string {
  return value.toLocaleString("zh-CN");
}

function isoDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}

function fillDays(days: number, trend: TrendPoint[]): TrendPoint[] {
  const byDate = new Map(trend.map((point) => [point.date, point]));
  const now = new Date();
  return Array.from({ length: days }, (_, index) => {
    const date = new Date(
      Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate() - (days - 1 - index)),
    );
    const key = isoDate(date);
    return byDate.get(key) ?? { date: key, launches: 0, activeDevices: 0, newDevices: 0 };
  });
}

function visibleRows<T extends { devices: number }>(rows: T[]): T[] {
  return rows.filter((row) => row.devices > 0).slice(0, 8);
}

function BarChart({ points }: { points: TrendPoint[] }) {
  const holder = useRef<HTMLDivElement>(null);
  const [width, setWidth] = useState(0);

  useEffect(() => {
    const element = holder.current;
    if (!element) return;
    const update = () => setWidth(element.clientWidth);
    update();
    const observer = new ResizeObserver(update);
    observer.observe(element);
    return () => observer.disconnect();
  }, []);

  const viewWidth = width > 0 ? width : 560;
  const height = width > 0 && width < 420 ? 230 : 290;
  const left = width > 0 && width < 420 ? 24 : 32;
  const right = 4;
  const top = 28;
  const bottom = 34;
  const plotWidth = Math.max(1, viewWidth - left - right);
  const plotHeight = Math.max(1, height - top - bottom);
  const max = Math.max(1, ...points.map((point) => point.activeDevices));
  const slot = plotWidth / Math.max(1, points.length);
  const barWidth = Math.max(4, Math.min(64, slot * 0.52));
  const radius = Math.min(barWidth / 2, 6);
  const tickCount = Math.max(1, Math.min(4, max));
  const labelEvery = Math.max(
    1,
    Math.ceil(points.length / Math.max(2, Math.floor(plotWidth / 62))),
  );

  return (
    <div className="chart-holder" ref={holder}>
      <svg
        className="bar-chart"
        width={viewWidth}
        height={height}
        viewBox={`0 0 ${viewWidth} ${height}`}
        role="img"
        aria-label="每日活跃设备柱状图"
      >
        {Array.from({ length: tickCount + 1 }, (_, index) => {
          const y = top + (index * plotHeight) / tickCount;
          const value = Math.round(max - (index * max) / tickCount);
          return (
            <g key={index}>
              <line
                x1={left}
                x2={viewWidth - right}
                y1={y}
                y2={y}
                className="bar-grid"
                strokeDasharray="3 4"
              />
              <text x={left - 7} y={y + 3.5} textAnchor="end" className="bar-axis">
                {value}
              </text>
            </g>
          );
        })}

        {points.map((point, index) => {
          const x = left + index * slot + (slot - barWidth) / 2;
          const ratio = point.activeDevices / max;
          const barHeight = point.activeDevices > 0 ? Math.max(6, ratio * plotHeight) : 0;
          const y = top + plotHeight - barHeight;
          const corner = Math.min(radius, barHeight / 2);
          const shape = [
            `M${x},${y + barHeight}`,
            `L${x},${y + corner}`,
            `Q${x},${y} ${x + corner},${y}`,
            `L${x + barWidth - corner},${y}`,
            `Q${x + barWidth},${y} ${x + barWidth},${y + corner}`,
            `L${x + barWidth},${y + barHeight}`,
            "Z",
          ].join(" ");
          return (
            <path
              key={point.date}
              d={shape}
              className={index === points.length - 1 ? "bar-latest" : "bar-shape"}
            >
              <title>
                {`${point.date} · ${number(point.activeDevices)} 台活跃设备 · ${number(
                  point.launches,
                )} 次启动`}
              </title>
            </path>
          );
        })}

        {points.map((point, index) => {
          if (index % labelEvery !== 0 && index !== points.length - 1) return null;
          return (
            <text
              key={point.date}
              x={left + index * slot + slot / 2}
              y={height - 12}
              textAnchor="middle"
              className="bar-axis"
            >
              {point.date.slice(5).replace("-", "/")}
            </text>
          );
        })}
      </svg>
    </div>
  );
}

function DistributionRows<T extends { devices: number }>({
  rows,
  label,
  icon,
}: {
  rows: T[];
  label: (row: T) => string;
  icon?: (row: T) => string | undefined;
}) {
  const shown = visibleRows(rows);
  if (shown.length === 0) {
    return <div className="rows-empty">还没有数据</div>;
  }
  const max = Math.max(1, ...shown.map((row) => row.devices));

  return (
    <div className="rows">
      {shown.map((row) => {
        const logo = icon?.(row);
        return (
          <div className="row" key={label(row)}>
            <div className="row-label">
              {logo ? <img className="row-logo" src={logo} alt="" /> : null}
              <span className="row-name">{label(row)}</span>
            </div>
            <div className="row-track">
              <span style={{ width: `${Math.max(3, (row.devices / max) * 100)}%` }} />
            </div>
            <span className="row-value">{number(row.devices)}</span>
          </div>
        );
      })}
    </div>
  );
}

function App() {
  const [days, setDays] = useState<RangeDays>(7);
  const [data, setData] = useState<PublicMetrics | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    let request: AbortController | null = null;

    const load = async () => {
      request?.abort();
      request = new AbortController();
      try {
        const response = await fetch(`/api/v1/public/metrics?days=${Math.max(days, 14)}`, {
          signal: request.signal,
        });
        if (!response.ok) throw new Error("统计服务暂时不可用");
        const next = (await response.json()) as PublicMetrics;
        if (active) {
          setData(next);
          setError("");
        }
      } catch (reason) {
        if (active && !(reason instanceof DOMException && reason.name === "AbortError")) {
          setError(reason instanceof Error ? reason.message : "加载失败");
        }
      }
    };

    void load();
    const timer = window.setInterval(() => void load(), 60_000);
    return () => {
      active = false;
      request?.abort();
      window.clearInterval(timer);
    };
  }, [days]);

  const points = useMemo(() => (data ? fillDays(days, data.trend) : []), [data, days]);

  const changes = useMemo(() => {
    const daily = data?.trend ?? [];
    const today = daily.at(-1);
    const yesterday = daily.at(-2);
    const newToday = data?.overview.newToday ?? 0;
    const deviceChange: Change = {
      dir: newToday > 0 ? "up" : "flat",
      text: newToday > 0 ? `+${number(newToday)}` : "0",
    };
    return {
      totalDevices: deviceChange,
      totalLaunches: percentChange(today?.launches ?? 0, yesterday?.launches ?? 0),
      activeToday: percentChange(today?.activeDevices ?? 0, yesterday?.activeDevices ?? 0),
      activeThisWeek: percentChange(
        average(daily.slice(-7).map((point) => point.activeDevices)),
        average(daily.slice(-14, -7).map((point) => point.activeDevices)),
      ),
    };
  }, [data]);

  return (
    <main className="home-shell">
      <section className="hero" style={{ backgroundImage: `url(${backgroundImage})` }}>
        <div className="hero-content">
          <h2>YumeBox</h2>
          <p className="hero-tagline">
            每一次启动，都是与你相遇的证明。 这里只留下版本与设备的模糊轮廓。
          </p>
        </div>
      </section>

      <section className="panel">
        <div className="dashboard-view">
          <header className="dashboard-header">
            <h1>Analytics</h1>
            <DropdownMenu>
              <DropdownMenuTrigger aria-label="切换时间范围" className="range-trigger">
                <div className="range-trigger-main">
                  <span className="range-trigger-icon" aria-hidden="true">
                    <Icon icon="calendar-range" iconLibrary="lucide" size={15} />
                  </span>
                  <span className="range-trigger-label">最近 {days} 天</span>
                </div>
                <div className="range-trigger-arrow" aria-hidden="true">
                  <span className="range-trigger-chevron" />
                </div>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="range-menu" sideOffset={8}>
                {RANGE_OPTIONS.map((option) => (
                  <DropdownMenuItem
                    key={option.days}
                    className="range-item"
                    isSelected={option.days === days}
                    onSelect={() => setDays(option.days)}
                  >
                    <span className="range-item-content">
                      <span className="range-item-icon" aria-hidden="true">
                        <Icon icon={option.icon} iconLibrary="lucide" size={16} />
                      </span>
                      <span className="range-item-copy">
                        <span className="range-item-title">{option.label}</span>
                        <span className="range-item-description">{option.description}</span>
                      </span>
                    </span>
                  </DropdownMenuItem>
                ))}
              </DropdownMenuContent>
            </DropdownMenu>
          </header>

          {error ? <div className="error-banner">{error}</div> : null}

          <div className="kpi-grid">
            {metricDefinitions.map((metric) => (
              <Card
                key={metric.key}
                className="kpi-card"
                title={metric.label}
                icon={<img className="card-icon-sm" src={metric.icon} alt="" />}
              >
                <div className="kpi-head">
                  <span className="kpi-value">
                    {data ? number(data.overview[metric.key]) : "—"}
                  </span>
                  <span className={`kpi-delta ${changes[metric.key].dir}`}>
                    <span className="kpi-delta-arrow">
                      {changes[metric.key].dir === "up"
                        ? "↗"
                        : changes[metric.key].dir === "down"
                          ? "↘"
                          : "→"}
                    </span>
                    {changes[metric.key].text}
                  </span>
                </div>
              </Card>
            ))}
          </div>

          <section className="chart-section">
            <div className="section-head">
              <h2>每日活跃设备</h2>
            </div>
            <BarChart points={points} />
          </section>

          <div className="split-grid">
            <Card
              className="panel-card"
              title="版本分布"
              icon={<img className="card-icon-lg" src={kotlinLogo} alt="" />}
            >
              <DistributionRows
                rows={data?.distributions.versions ?? []}
                label={(row) => row.version}
              />
            </Card>

            <Card
              className="panel-card"
              title="Android 版本"
              icon={<img className="card-icon-lg" src={androidLogo} alt="" />}
            >
              <DistributionRows
                rows={data?.distributions.androidVersion ?? []}
                label={(row) => `Android ${row.value}`}
              />
            </Card>
          </div>

          <Card className="panel-card oem-card">
            <DistributionRows
              rows={data?.distributions.manufacturer ?? []}
              label={(row) => row.value}
              icon={(row) => oemLogo(row.value)}
            />
          </Card>
        </div>
      </section>
    </main>
  );
}

export default App;
