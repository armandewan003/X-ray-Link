import { Module } from '@nestjs/common';
import { AuthModule } from './modules/auth/auth.module';
import { AdminsModule } from './modules/admins/admins.module';
import { ResellersModule } from './modules/resellers/resellers.module';
import { UsersModule } from './modules/users/users.module';
import { CreditsModule } from './modules/credits/credits.module';
import { ServersModule } from './modules/servers/servers.module';
import { ProfilesModule } from './modules/profiles/profiles.module';
import { SessionsModule } from './modules/sessions/sessions.module';
import { TrafficModule } from './modules/traffic/traffic.module';
import { ConfigModule } from './modules/config/config.module';
import { AuditLogsModule } from './modules/audit-logs/audit-logs.module';
import { RealtimeGateway } from './modules/realtime/realtime.gateway';

@Module({
  imports: [
    AuthModule,
    AdminsModule,
    ResellersModule,
    UsersModule,
    CreditsModule,
    ServersModule,
    ProfilesModule,
    SessionsModule,
    TrafficModule,
    ConfigModule,
    AuditLogsModule,
  ],
  providers: [RealtimeGateway],
})
export class AppModule {}
