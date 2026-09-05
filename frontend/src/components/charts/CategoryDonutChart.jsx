import {
  PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer
} from 'recharts';

const PALETTE = {
  WORK:       '#9B7FFF',
  PERSONAL:   '#4FC3F7',
  FINANCE:    '#4ADE80',
  PROMOTIONS: '#FACC15',
  SOCIAL:     '#F7C84F',
  SPAM:       '#FF5C7A',
  UPDATES:    '#818CF8',
  OTHER:      '#94A3B8',
};

const CustomTooltip = ({ active, payload }) => {
  if (!active || !payload?.length) return null;
  const { name, value } = payload[0];
  return (
    <div style={{
      background: 'var(--bg-elevated)',
      border: 'var(--border-glass)',
      borderRadius: 'var(--radius-md)',
      padding: '10px 14px',
      fontSize: '0.82rem',
      boxShadow: 'var(--shadow-md)',
    }}>
      <p style={{ color: 'var(--text-primary)', fontWeight: 600, marginBottom: 2 }}>
        {name.charAt(0) + name.slice(1).toLowerCase()}
      </p>
      <p style={{ color: 'var(--text-muted)' }}>{value.toLocaleString()} emails</p>
    </div>
  );
};

const renderCustomLabel = ({ cx, cy, midAngle, innerRadius, outerRadius, percent }) => {
  if (percent < 0.05) return null;
  const RADIAN = Math.PI / 180;
  const r = innerRadius + (outerRadius - innerRadius) * 0.55;
  const x = cx + r * Math.cos(-midAngle * RADIAN);
  const y = cy + r * Math.sin(-midAngle * RADIAN);
  return (
    <text x={x} y={y} fill="white" textAnchor="middle" dominantBaseline="central"
      style={{ fontSize: '0.72rem', fontWeight: 700, fontFamily: 'Outfit, sans-serif' }}>
      {`${(percent * 100).toFixed(0)}%`}
    </text>
  );
};

export default function CategoryDonutChart({ countByCategory }) {
  const data = Object.entries(countByCategory || {})
    .filter(([, v]) => v > 0)
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value);

  if (!data.length) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center',
        height: 260, color: 'var(--text-muted)', fontSize: '0.85rem' }}>
        No category data yet
      </div>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={280}>
      <PieChart>
        <Pie
          data={data}
          cx="50%"
          cy="50%"
          innerRadius={72}
          outerRadius={108}
          dataKey="value"
          labelLine={false}
          label={renderCustomLabel}
          strokeWidth={2}
          stroke="var(--bg-base)"
          animationBegin={0}
          animationDuration={900}
        >
          {data.map((entry) => (
            <Cell key={entry.name} fill={PALETTE[entry.name] ?? '#94A3B8'} />
          ))}
        </Pie>
        <Tooltip content={<CustomTooltip />} />
        <Legend
          iconType="circle"
          iconSize={8}
          formatter={(value) =>
            <span style={{ color: 'var(--text-secondary)', fontSize: '0.78rem' }}>
              {value.charAt(0) + value.slice(1).toLowerCase()}
            </span>
          }
        />
      </PieChart>
    </ResponsiveContainer>
  );
}
